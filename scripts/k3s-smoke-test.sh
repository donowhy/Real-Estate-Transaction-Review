#!/usr/bin/env bash
set -Eeuo pipefail

KUBECONFIG_PATH="${KUBECONFIG_PATH:-$HOME/.kube/config}"
KUBE_CONTEXT="${KUBE_CONTEXT:-rancher-desktop}"
NAMESPACE="${NAMESPACE:-logistics}"
ORDER_PORT="${ORDER_PORT:-18091}"
INVENTORY_PORT="${INVENTORY_PORT:-18092}"
TRAEFIK_PORT="${TRAEFIK_PORT:-18080}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-90}"
RUN_ID="$(date +%s)"
SKU="K3S-SMOKE-$RUN_ID"
MISSING_SKU="K3S-MISSING-$RUN_ID"
PORT_FORWARD_PIDS=()

kubectl_cmd() {
  KUBECONFIG="$KUBECONFIG_PATH" kubectl --context "$KUBE_CONTEXT" "$@"
}

cleanup() {
  for pid in "${PORT_FORWARD_PIDS[@]}"; do
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" 2>/dev/null || true
  done
}

diagnostics() {
  local exit_code=$?
  trap - ERR
  printf '\nSmoke test failed. Collecting diagnostics...\n' >&2
  kubectl_cmd -n "$NAMESPACE" get pods -o wide >&2 || true
  kubectl_cmd -n "$NAMESPACE" get events --sort-by=.lastTimestamp | tail -60 >&2 || true
  for service in order-service inventory-service payment-service delivery-service notification-service; do
    kubectl_cmd -n "$NAMESPACE" logs "deployment/$service" --tail=120 --prefix=true >&2 || true
    kubectl_cmd -n "$NAMESPACE" logs "deployment/$service" --previous --tail=120 --prefix=true >&2 || true
  done
  kubectl_cmd -n kube-system logs deployment/traefik --tail=120 >&2 || true
  exit "$exit_code"
}
trap cleanup EXIT
trap diagnostics ERR

command -v curl >/dev/null
command -v jq >/dev/null
command -v kubectl >/dev/null

for deployment in logistics-postgres redis kafka order-service inventory-service payment-service delivery-service notification-service; do
  kubectl_cmd -n "$NAMESPACE" rollout status "deployment/$deployment" --timeout=30s >/dev/null
done

kubectl_cmd -n "$NAMESPACE" port-forward service/order-service "$ORDER_PORT:80" >/tmp/logistics-order-port-forward.log 2>&1 &
PORT_FORWARD_PIDS+=("$!")
kubectl_cmd -n "$NAMESPACE" port-forward service/inventory-service "$INVENTORY_PORT:80" >/tmp/logistics-inventory-port-forward.log 2>&1 &
PORT_FORWARD_PIDS+=("$!")
kubectl_cmd -n kube-system port-forward service/traefik "$TRAEFIK_PORT:80" >/tmp/logistics-traefik-port-forward.log 2>&1 &
PORT_FORWARD_PIDS+=("$!")

wait_for_http() {
  local url=$1
  local deadline=$((SECONDS + 30))
  until curl --silent --output /dev/null "$url"; do
    if (( SECONDS >= deadline )); then
      printf 'Timed out waiting for %s\n' "$url" >&2
      return 1
    fi
    sleep 1
  done
}

wait_for_http "http://127.0.0.1:$ORDER_PORT/actuator/health"
wait_for_http "http://127.0.0.1:$INVENTORY_PORT/actuator/health"
wait_for_http "http://127.0.0.1:$TRAEFIK_PORT/ping"

curl --fail --silent --show-error \
  -X POST "http://127.0.0.1:$INVENTORY_PORT/internal/v1/inventory/stock" \
  -H 'Content-Type: application/json' \
  -d "{\"sku\":\"$SKU\",\"quantity\":10}" >/dev/null

create_order() {
  local idempotency_key=$1
  local sku=$2
  curl --fail --silent --show-error \
    -X POST "http://127.0.0.1:$ORDER_PORT/api/v1/orders" \
    -H 'Content-Type: application/json' \
    -d "{\"idempotencyKey\":\"$idempotency_key\",\"sku\":\"$sku\",\"quantity\":2,\"amount\":25000,\"deliveryAddress\":\"Seoul\"}"
}

wait_for_status() {
  local order_id=$1
  local expected_status=$2
  local deadline=$((SECONDS + TIMEOUT_SECONDS))
  local response status

  while (( SECONDS < deadline )); do
    response="$(curl --fail --silent --show-error \
      "http://127.0.0.1:$ORDER_PORT/api/v1/orders/$order_id")"
    status="$(jq -r '.data.status' <<<"$response")"
    if [[ "$status" == "$expected_status" ]]; then
      return 0
    fi
    sleep 2
  done

  printf 'Order %s did not reach %s. Last status: %s\n' \
    "$order_id" "$expected_status" "$status" >&2
  return 1
}

success_key="k3s-success-$RUN_ID"
success_response="$(create_order "$success_key" "$SKU")"
success_order_id="$(jq -er '.data.id' <<<"$success_response")"
wait_for_status "$success_order_id" CONFIRMED

duplicate_response="$(create_order "$success_key" "$SKU")"
duplicate_order_id="$(jq -er '.data.id' <<<"$duplicate_response")"
if [[ "$success_order_id" != "$duplicate_order_id" ]]; then
  printf 'Idempotent requests returned different order IDs.\n' >&2
  exit 1
fi

failed_response="$(create_order "k3s-failure-$RUN_ID" "$MISSING_SKU")"
failed_order_id="$(jq -er '.data.id' <<<"$failed_response")"
wait_for_status "$failed_order_id" CANCELLED

postgres_pod="$(kubectl_cmd -n "$NAMESPACE" get pod -l app=logistics-postgres -o jsonpath='{.items[0].metadata.name}')"
db_user="$(kubectl_cmd -n "$NAMESPACE" get secret logistics-secrets -o jsonpath='{.data.DB_USERNAME}' | base64 --decode)"

query_count() {
  local database=$1
  local sql=$2
  kubectl_cmd -n "$NAMESPACE" exec "$postgres_pod" -- \
    psql -U "$db_user" -d "$database" -Atc "$sql"
}

unpublished=0
for database in orderdb inventorydb paymentdb deliverydb notificationdb; do
  database_unpublished="$(query_count "$database" \
    'select count(*) from outbox_event where published_at is null;')"
  unpublished=$((unpublished + database_unpublished))
done

processed=0
for database in orderdb inventorydb paymentdb deliverydb notificationdb; do
  database_processed="$(query_count "$database" 'select count(*) from processed_event;')"
  processed=$((processed + database_processed))
done

notifications="$(query_count notificationdb \
  "select count(*) from notification where order_id in ('$success_order_id', '$failed_order_id');")"

if [[ "$unpublished" != "0" ]]; then
  printf 'Service databases contain %s unpublished outbox events.\n' "$unpublished" >&2
  exit 1
fi
if (( processed < 2 )); then
  printf 'Combined processed_event count is unexpectedly low: %s\n' "$processed" >&2
  exit 1
fi
if [[ "$notifications" != "2" ]]; then
  printf 'Expected 2 notification rows, found %s.\n' "$notifications" >&2
  exit 1
fi

kubectl_cmd -n "$NAMESPACE" logs deployment/notification-service --tail=200 |
  grep -q "notification sent orderId=$success_order_id eventType=ORDER_CONFIRMED"

traefik_response="$(curl --fail --silent --show-error \
  -H 'Host: logistics.local' \
  "http://127.0.0.1:$TRAEFIK_PORT/api/v1/orders/$success_order_id")"
if [[ "$(jq -r '.data.status' <<<"$traefik_response")" != "CONFIRMED" ]]; then
  printf 'Traefik did not return the confirmed order.\n' >&2
  exit 1
fi

printf 'Smoke test passed.\n'
printf 'Confirmed order: %s\n' "$success_order_id"
printf 'Cancelled order: %s\n' "$failed_order_id"
printf 'Idempotency returned the same order ID: %s\n' "$duplicate_order_id"
