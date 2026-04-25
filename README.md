# Risk Engine (Spring Boot, Reactive)

Reactive risk engine for algorithmic trading built on Java 21 and Spring Boot 3.

## Features

- **Core risk checks**
  - Position limit check per instrument
  - Order rate limiter per trader (orders/sec)
  - PnL drawdown guard (halts with reject)
  - Total market exposure check
  - Duplicate order detection
- **Event-driven flow**
  - Consumes order events from Kafka topic `order-events`
  - Publishes risk decisions to Kafka topic `risk-decisions`
  - Routes accepted orders to IBM MQ queue `FIX.ORDER.ROUTE`
- **State and persistence**
  - Redis (positions, exposure, rate limiting, duplicate keys, pnl snapshots)
  - Redis pipelined pre-trade snapshot fetch (single round trip for hot checks)
  - PostgreSQL via R2DBC (`trade_history`, `audit_log`)
- **Observability**
  - Micrometer counters: `order.accepted`, `order.rejected`, `risk.breach{check=...}`
  - Micrometer tracing with OTLP export
  - Structured JSON logging with trace/span ids
  - Pre-trade latency metric: `risk.pretrade.pipeline.latency`
- **Resilience**
  - Resilience4j circuit breakers on Kafka publish, IBM MQ routing, and Postgres writes

## Stack

- Java 21
- Spring Boot 3 + WebFlux
- Redis
- Kafka
- IBM MQ
- PostgreSQL (R2DBC)
- Micrometer + OTLP

## Run locally

```bash
mvn spring-boot:run
```

Environment variables:

- `KAFKA_BOOTSTRAP_SERVERS`
- `REDIS_HOST`, `REDIS_PORT`
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `IBM_MQ_QUEUE_MANAGER`, `IBM_MQ_CHANNEL`, `IBM_MQ_CONN_NAME`, `IBM_MQ_USER`, `IBM_MQ_PASSWORD`
- `OTLP_METRICS_URL`, `OTLP_TRACES_URL`

## API

- `POST /api/risk/evaluate`
  - request: `OrderEvent`
  - response: `RiskDecision`

Example payload:

```json
{
  "orderId": "ord-1001",
  "traderId": "trdr-42",
  "instrumentId": "AAPL",
  "side": "BUY",
  "quantity": 100,
  "price": 183.25,
  "eventTime": "2026-04-26T00:00:00Z"
}
```

## Tests

- Unit tests:
  - `DuplicateOrderCheckTest`
  - `PositionLimitCheckTest`
- Integration test:
  - `RiskControllerIntegrationTest`

Run tests:

```bash
mvn test
```

## Docker

Multi-stage Docker build:

```bash
docker build -t risk-engine:local .
docker run -p 8080:8080 risk-engine:local
```

## Kubernetes

Base manifests in `k8s/base`:

- `deployment.yaml`
- `serviceaccount.yaml` (GKE Workload Identity)
- `service.yaml`
- `hpa.yaml`
- `vpa.yaml`

Apply:

```bash
kubectl apply -f k8s/base
```

## Helm

Chart location: `helm/risk-engine`

```bash
helm upgrade --install risk-engine helm/risk-engine
```

Workload Identity is enabled via:

- `gcp.workloadIdentity.enabled`
- `gcp.workloadIdentity.kubernetesServiceAccount`
- `gcp.workloadIdentity.gcpServiceAccount`

## Terraform (GCP)

Terraform is included in `infra/terraform` and provisions:

- GKE Autopilot cluster
- Cloud Memorystore (Redis)
- Cloud SQL PostgreSQL
- Artifact Registry repository
- Runtime GCP service account + Workload Identity binding

Basic usage:

```bash
cd infra/terraform
terraform init
terraform apply -var="project_id=<your-project-id>" -var="postgres_password=<secure-password>"
```

## GitHub Actions CI/CD

- `ci.yml`: runs Maven build/tests on PRs and `main`.
- `cd-gke.yml`: uses Workload Identity Federation, builds/pushes image, and deploys Helm to GKE.

Required GitHub secrets:

- `GCP_PROJECT_ID`
- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_DEPLOYER_SA`
- `GCP_RUNTIME_SA`
- `GAR_REPOSITORY`

## GCP Deployment Notes

- **GKE Autopilot**: deploy chart/manifests as-is with workload resources.
- **Workload Identity**: use KSA/GSA binding; no service account key files are required.
- **Cloud Memorystore (Redis)**: set `REDIS_HOST` to Memorystore private IP.
- **Cloud SQL (PostgreSQL)**:
  - use private IP or Cloud SQL Auth Proxy sidecar
  - set DB vars in chart values or a Secret
- Use Artifact Registry/GCR image path in Helm values (`image.repository`).
