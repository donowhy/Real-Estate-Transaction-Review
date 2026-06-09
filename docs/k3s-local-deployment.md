# 로컬 k3s 배포 가이드

이 가이드는 Rancher Desktop k3s 환경에서 물류 MSA를 실행하는 방법을 설명합니다. 이는 로컬 개발을 위한 단일 노드 설정이며, 운영 환경의 고가용성 배포를 위한 설정은 아닙니다.

## 사전 요구 사항

- Rancher Desktop (Moby 컨테이너 엔진 및 Kubernetes 활성화 상태)
- Rancher Desktop에 할당된 최소 리소스: CPU 4개, RAM 8GB
- Java 21, Docker CLI, kubectl, curl, jq 설치
- `$HOME/.kube/config`에 `rancher-desktop` 컨텍스트 설정 완료

모든 명령어는 명시적으로 `rancher-desktop` 컨텍스트를 사용합니다. 이는 이전의 다른 로컬 클러스터로 인해 남은 잘못된 `KUBECONFIG` 설정을 방지하기 위함입니다.

자동화 스크립트는 환경 변수 `KUBECONFIG`를 무시하고 기본적으로 `$HOME/.kube/config`를 사용합니다. 다른 파일을 사용해야 하는 경우에만 `KUBECONFIG_PATH`를 설정하세요.

```bash
KUBECONFIG="$HOME/.kube/config" \
  kubectl --context rancher-desktop get nodes
```

## 배포 방법

로컬 전용 데이터베이스 비밀번호를 설정합니다. 스크립트는 비밀번호를 리포지토리에 기록하지 않고 `logistics-secrets` Secret을 생성하거나 업데이트합니다.

```bash
export LOGISTICS_DB_PASSWORD='local-development-password'
./scripts/k3s-build-deploy.sh
```

이 스크립트는 다음 과정을 수행합니다:

1. kubeconfig 및 컨텍스트 유효성 검사
2. 기존 애플리케이션 파드(Pod)의 스케일을 0으로 조정
3. 모든 Gradle 테스트 실행 및 5개 서비스의 JAR 빌드
4. 로컬 `myservice/*:0.1.0` 이미지 빌드
5. Kubernetes 매니페스트 적용
6. Postgres, Redis, Kafka 상태 확인 및 대기
7. Saga 의존성 순서에 따라 각 애플리케이션 서비스 시작

애플리케이션 매니페스트는 의도적으로 `replicas: 0`으로 선언되어 있습니다. 반드시 배포 스크립트를 사용하여 필요한 순서대로 시작해야 합니다.

## 검증 방법

```bash
./scripts/k3s-smoke-test.sh
```

스모크 테스트(Smoke Test)는 다음 항목을 검증합니다:

- 재고 생성 확인
- 성공적인 주문이 `CONFIRMED` 상태에 도달하는지 확인
- 존재하지 않는 SKU 주문이 `CANCELLED` 상태로 처리되는지 확인
- 중복된 멱등성 키(Idempotency Key)가 동일한 주문 ID를 반환하는지 확인
- Outbox 행 발행 및 컨슈머의 `processed_event` 기록 확인
- 알림 데이터 및 로그 생성 확인

클러스터 상태 직접 확인:

```bash
KUBECONFIG="$HOME/.kube/config" \
  kubectl --context rancher-desktop -n logistics get pods
```

Traefik 라우트 테스트:

```bash
curl -H 'Host: logistics.local' \
  http://127.0.0.1/api/v1/orders/<order-id>
```

만약 Rancher Desktop이 localhost가 아닌 VM 주소로 Traefik을 노출하는 경우, 다음 명령어로 확인된 `ADDRESS`를 사용하세요:

```bash
KUBECONFIG="$HOME/.kube/config" \
  kubectl --context rancher-desktop -n logistics get ingress logistics-api
```

## 재배포

`./scripts/k3s-build-deploy.sh`를 다시 실행하세요. 스크립트는 로컬 이미지 태그를 교체하기 전에 애플리케이션 파드 규모를 축소하여, 재빌드 후에도 이전의 캐시된 컨테이너가 남아있지 않도록 방지합니다.

## 중지 및 삭제

의존성 라이브러리와 데이터는 유지하면서 애플리케이션 서비스만 중지:

```bash
KUBECONFIG="$HOME/.kube/config" kubectl --context rancher-desktop \
  -n logistics scale deployment \
  order-service inventory-service payment-service delivery-service notification-service \
  --replicas=0
```

네임스페이스 및 로컬 Postgres 볼륨 삭제:

```bash
KUBECONFIG="$HOME/.kube/config" \
  kubectl --context rancher-desktop delete namespace logistics
```

## 문제 해결 (Troubleshooting)

명령어 실패 시 스크립트가 파드 상태, 이벤트, 현재 로그 및 이전 컨테이너 로그를 출력합니다. 수동 확인 명령어는 다음과 같습니다:

```bash
# 이벤트 확인
KUBECONFIG="$HOME/.kube/config" kubectl --context rancher-desktop \
  -n logistics get events --sort-by=.lastTimestamp

# 실시간 로그 확인
KUBECONFIG="$HOME/.kube/config" kubectl --context rancher-desktop \
  -n logistics logs deployment/order-service --tail=200

# 이전 컨테이너 로그 확인 (재시작된 경우)
KUBECONFIG="$HOME/.kube/config" kubectl --context rancher-desktop \
  -n logistics logs deployment/order-service --previous --tail=200
```

만약 애플리케이션이 `REDIS_PORT` 값으로 `tcp://...`와 같은 형식을 받는다면, 파드 스펙에 `enableServiceLinks: false`가 설정되어 있는지 확인하세요.

kubectl에서 `127.0.0.1:6443` 연결 오류가 발생하면 Rancher Desktop의 Kubernetes가 실행 중인지 확인하고, `rancher-desktop` 컨텍스트가 포함된 `$HOME/.kube/config`를 사용 중인지 확인하세요.

Rancher Desktop 시작 시 `No active cluster` 오류가 발생하며 실패하는 경우, 컨텍스트를 먼저 선택한 후 다시 시도하세요:

```bash
KUBECONFIG="$HOME/.kube/config" \
  kubectl config use-context rancher-desktop
```

VM 내부에서 `k3s` 서비스만 단독으로 재시작하지 마세요. Rancher Desktop은 k3s가 시작되기 전에 `rd1` 인터페이스를 생성하므로, 이 순서를 무시하면 flannel이 종료되어 VM이 `Broken` 상태가 될 수 있습니다. 데이터 삭제 없이 복구하는 방법:

```bash
rdctl shutdown
open -a 'Rancher Desktop'
```

VM 네트워크와 Kubernetes가 모두 준비될 때까지 기다리세요:

```bash
rdctl shell -- ip -4 addr show rd1
KUBECONFIG="$HOME/.kube/config" \
  kubectl --context rancher-desktop get nodes
```

Traefik이 `503` 또는 `404`를 반환하지만 서비스 엔드포인트가 준비된 상태라면, Traefik이 Kubernetes API를 감시할 수 있는지 확인하세요:

```bash
KUBECONFIG="$HOME/.kube/config" kubectl --context rancher-desktop \
  -n kube-system logs deployment/traefik --tail=120
```

`10.43.0.1:443` 접속 실패가 반복되면 서비스 VIP 네트워킹 문제일 가능성이 높습니다. k3s 단독 재시작 대신 위에 설명한 Rancher Desktop 전체 재시작을 수행하세요.

운영 환경에서는 로컬 이미지를 레지스트리로 교체하고, 관리형 또는 오퍼레이터 기반의 Kafka를 사용하며, 스키마 마이그레이션 적용 및 외부 시크릿 관리 도구를 사용해야 합니다.
