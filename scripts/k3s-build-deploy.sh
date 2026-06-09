#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-$HOME/.kube/config}"
KUBE_CONTEXT="${KUBE_CONTEXT:-rancher-desktop}"
NAMESPACE="${NAMESPACE:-logistics}"
IMAGE_TAG="${IMAGE_TAG:-0.1.0}"
SERVICES=(
  order-service
  inventory-service
  payment-service
  delivery-service
  notification-service
)
DEPENDENCIES=(logistics-postgres redis kafka)

kubectl_cmd() {
  KUBECONFIG="$KUBECONFIG_PATH" kubectl --context "$KUBE_CONTEXT" "$@"
}

diagnostics() {
  local exit_code=$?
  trap - ERR
  printf '\nDeployment failed. Collecting diagnostics...\n' >&2
  kubectl_cmd -n "$NAMESPACE" get pods -o wide >&2 || true
  kubectl_cmd -n "$NAMESPACE" get events --sort-by=.lastTimestamp | tail -60 >&2 || true
  for service in "${SERVICES[@]}"; do
    kubectl_cmd -n "$NAMESPACE" logs "deployment/$service" --tail=200 --prefix=true >&2 || true
    kubectl_cmd -n "$NAMESPACE" logs "deployment/$service" --previous --tail=200 --prefix=true >&2 || true
  done
  exit "$exit_code"
}
trap diagnostics ERR

command -v docker >/dev/null
command -v kubectl >/dev/null

if [[ ! -f "$KUBECONFIG_PATH" ]]; then
  printf 'Kubeconfig not found: %s\n' "$KUBECONFIG_PATH" >&2
  exit 1
fi

if ! KUBECONFIG="$KUBECONFIG_PATH" kubectl config get-contexts "$KUBE_CONTEXT" \
  --no-headers 2>/dev/null | grep -q "$KUBE_CONTEXT"; then
  printf 'Context %s was not found in %s.\n' "$KUBE_CONTEXT" "$KUBECONFIG_PATH" >&2
  exit 1
fi

if ! kubectl_cmd get --raw=/readyz >/dev/null 2>&1; then
  printf 'Kubernetes context %s is not ready.\n' "$KUBE_CONTEXT" >&2
  printf 'Start Rancher Desktop and ensure ~/.kube/config uses the rancher-desktop context.\n' >&2
  exit 1
fi

if [[ -n "${LOGISTICS_DB_PASSWORD:-}" ]]; then
  kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/namespace.yaml"
  kubectl_cmd -n "$NAMESPACE" create secret generic logistics-secrets \
    --from-literal=DB_USERNAME="${LOGISTICS_DB_USERNAME:-logistics}" \
    --from-literal=DB_PASSWORD="$LOGISTICS_DB_PASSWORD" \
    --dry-run=client -o yaml | kubectl_cmd apply -f -
elif ! kubectl_cmd -n "$NAMESPACE" get secret logistics-secrets >/dev/null 2>&1; then
  printf 'Set LOGISTICS_DB_PASSWORD or create the logistics-secrets Secret first.\n' >&2
  exit 1
fi

printf 'Scaling application deployments to zero...\n'
for service in "${SERVICES[@]}"; do
  kubectl_cmd -n "$NAMESPACE" scale "deployment/$service" --replicas=0 >/dev/null 2>&1 || true
done

printf 'Running Gradle tests and building service jars...\n'
(
  cd "$ROOT_DIR"
  ./gradlew test
  ./gradlew \
    :order-service:bootJar \
    :inventory-service:bootJar \
    :payment-service:bootJar \
    :delivery-service:bootJar \
    :notification-service:bootJar
)

printf 'Building Rancher Desktop images...\n'
for service in "${SERVICES[@]}"; do
  docker build \
    --build-arg "SERVICE=$service" \
    -f "$ROOT_DIR/infra/docker/logistics-runtime.Dockerfile" \
    -t "myservice/$service:$IMAGE_TAG" \
    "$ROOT_DIR"
done

printf 'Applying Kubernetes manifests...\n'
kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/namespace.yaml"
kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/configmap.yaml"
kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/dependencies.yaml"
kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/apps.yaml"
kubectl_cmd apply -f "$ROOT_DIR/infra/k8s/logistics/ingress.yaml"

for dependency in "${DEPENDENCIES[@]}"; do
  kubectl_cmd -n "$NAMESPACE" rollout status "deployment/$dependency" --timeout=180s
done

printf 'Starting application services in dependency order...\n'
for service in "${SERVICES[@]}"; do
  kubectl_cmd -n "$NAMESPACE" scale "deployment/$service" --replicas=1
  kubectl_cmd -n "$NAMESPACE" rollout status "deployment/$service" --timeout=240s
done

kubectl_cmd -n "$NAMESPACE" get pods -o wide
printf '\nDeployment complete. Run scripts/k3s-smoke-test.sh to verify the Saga.\n'
