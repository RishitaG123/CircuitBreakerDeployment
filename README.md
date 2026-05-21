# Microservices Deployment Pipeline

This repository contains a complete CI/CD pipeline for deploying microservices to Google Kubernetes Engine (GKE) with Cloud SQL PostgreSQL integration using Google Cloud Build.

## Architecture

The system consists of three Spring Boot microservices:

1. **Order Service** (Port 8080) - Main orchestration service with LoadBalancer
2. **Fraud Service** (Port 8081) - Fraud detection service with PostgreSQL
3. **Payment Service** (Port 8082) - Payment processing service with PostgreSQL

## Features

- **Docker Images**: Multi-stage builds for all microservices
- **Artifact Registry**: Images pushed to Google Artifact Registry
- **GKE Deployment**: Automated cluster creation and deployment
- **Cloud SQL Integration**: PostgreSQL database for microservices
- **Service Mesh**: Internal service communication via Kubernetes services
- **Load Balancing**: External access via LoadBalancer for order-service

## Prerequisites

1. Google Cloud Project with billing enabled
2. Cloud Build API enabled
3. Artifact Registry API enabled
4. Kubernetes Engine API enabled
5. Cloud SQL Admin API enabled
6. Appropriate IAM permissions for Cloud Build service account

## Configuration

The pipeline is configured via substitutions in `cloudbuild.yaml`:

```yaml
substitutions:
  _REGION: "us-central1"                    # Artifact Registry and GKE region
  _REPOSITORY: "microservices-repo"         # Artifact Registry repository name
  _CLUSTER_NAME: "cb-cluster"               # GKE cluster name
  _CLUSTER_ZONE: "us-central1-a"            # GKE cluster zone
  _DB_INSTANCE: "cb-sql-instance"           # Cloud SQL instance name
  _DB_REGION: "us-central1"                 # Cloud SQL region
  _DB_NAME: "payments"                      # Database name
  _DB_USER: "postgres"                      # Database user
  _DB_PASS: "123"                           # Database password (change in production)
  _DB_IP: ""                                # Optional: explicit DB IP (auto-detected if empty)
```

## Deployment

### Manual Trigger

```bash
gcloud builds submit --config cloudbuild.yaml .
```

### With Custom Parameters

```bash
gcloud builds submit --config cloudbuild.yaml \
  --substitutions=_CLUSTER_NAME=my-cluster,_DB_PASS=secure-password .
```

### Automated Trigger

Create a Cloud Build trigger connected to your repository:

```bash
gcloud builds triggers create github \
  --repo-name=your-repo \
  --repo-owner=your-username \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml
```

## Pipeline Steps

1. **Create Artifact Registry Repository** - Creates Docker repository (idempotent)
2. **Build Docker Images** - Builds all three microservice images
3. **Push to Artifact Registry** - Pushes images to Google Artifact Registry
4. **Create GKE Cluster** - Creates Kubernetes cluster (idempotent)
5. **Get GKE Credentials** - Configures kubectl access
6. **Create Cloud SQL Instance** - Creates PostgreSQL instance (idempotent)
7. **Deploy to Kubernetes** - Applies manifests and creates secrets
8. **Wait for LoadBalancer** - Waits for external IP assignment

## Database Integration

The fraud-service and payment-service connect to Cloud SQL PostgreSQL using:

- **Connection**: Direct IP connection (public IP)
- **Credentials**: Kubernetes secrets (`db-credentials`)
- **Configuration**: Environment variables injected into pods

### Database Schema

Each service manages its own database schema using Spring JPA with `hibernate.ddl-auto=update`.

## Service Communication

- **External**: order-service exposed via LoadBalancer on port 80
- **Internal**: Services communicate via Kubernetes DNS
  - `http://fraud-service:8081`
  - `http://payment-service:8082`

## Monitoring and Troubleshooting

### Check Deployment Status

```bash
kubectl get pods
kubectl get services
kubectl get deployments
```

### View Logs

```bash
kubectl logs -l app=order-service
kubectl logs -l app=fraud-service
kubectl logs -l app=payment-service
```

### Access Application

```bash
# Get external IP
kubectl get service order-service

# Test endpoint
curl http://EXTERNAL_IP/api/orders
```

### Database Connection

```bash
# Check database secrets
kubectl get secret db-credentials -o yaml

# Test database connectivity
kubectl exec -it deployment/fraud-service -- env | grep SPRING_DATASOURCE
```

## Security Considerations

1. **Database Password**: Change default password in production
2. **Network Security**: Consider private GKE clusters and Cloud SQL private IP
3. **Service Accounts**: Use Workload Identity for pod-to-GCP authentication
4. **Secrets Management**: Consider Google Secret Manager integration
5. **Image Security**: Scan images for vulnerabilities

## Cleanup

```bash
# Delete GKE cluster
gcloud container clusters delete cb-cluster --zone us-central1-a

# Delete Cloud SQL instance
gcloud sql instances delete cb-sql-instance

# Delete Artifact Registry repository
gcloud artifacts repositories delete microservices-repo --location us-central1
```

## Development

### Local Development

Each service can be run locally with:

```bash
cd order-service
mvn spring-boot:run
```

### Testing

```bash
# Build and test locally
mvn clean test

# Integration testing
docker-compose up -d postgres
mvn test -Dspring.profiles.active=test
```

## Troubleshooting

### Common Issues

1. **IAM Permissions**: Ensure Cloud Build service account has necessary roles
2. **API Enablement**: Verify all required APIs are enabled
3. **Resource Quotas**: Check GCP quotas for compute, SQL, and networking
4. **Image Pull**: Verify Artifact Registry permissions for GKE nodes

### Error Resolution

- **"DB_IP" invalid substitution**: Fixed by using proper environment variable export
- **Image pull errors**: Ensure GKE has access to Artifact Registry
- **Database connection**: Verify Cloud SQL IP and firewall rules
- **Service discovery**: Check Kubernetes DNS and service configurations