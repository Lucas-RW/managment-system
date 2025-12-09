# Patient Management System

A production-ready, cloud-native microservices-based patient management system built with Spring Boot and deployed on AWS infrastructure. This system demonstrates modern software engineering practices including event-driven architecture, gRPC-based inter-service communication, and comprehensive infrastructure as code.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Microservices](#microservices)
- [Infrastructure](#infrastructure)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Communication Patterns](#communication-patterns)
- [Security](#security)
- [Testing](#testing)
- [Deployment](#deployment)
- [Development](#development)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

## Overview

The Patient Management System is a distributed healthcare application designed to manage patient records, authentication, billing, and analytics. The system leverages a microservices architecture to ensure scalability, maintainability, and independent deployment of services.

### Key Features

- **Patient Management**: Full CRUD operations for patient records
- **Authentication & Authorization**: JWT-based secure authentication
- **Billing Integration**: Automated billing account creation via gRPC
- **Real-time Analytics**: Event-driven analytics using Apache Kafka
- **API Gateway**: Centralized routing and JWT validation
- **Cloud-Native**: Fully containerized and AWS ECS-ready
- **Infrastructure as Code**: AWS CDK for automated infrastructure provisioning

## Architecture

The system follows a **distributed microservices architecture** with the following design principles:

- **Service Independence**: Each microservice can be developed, deployed, and scaled independently
- **Event-Driven Communication**: Asynchronous messaging via Apache Kafka for loosely coupled services
- **Synchronous RPC**: gRPC for low-latency, strongly-typed inter-service communication
- **API Gateway Pattern**: Single entry point for client requests with centralized authentication
- **Database per Service**: Each service manages its own PostgreSQL database

### Architecture Diagram

```
┌─────────────┐
│   Clients   │
└──────┬──────┘
       │
       ▼
┌─────────────────────┐
│   API Gateway       │ (Port 4004)
│   Load Balancer     │
└──────┬──────────────┘
       │
       ├──────────────────┬────────────────────┐
       ▼                  ▼                    ▼
┌─────────────┐    ┌──────────────┐    ┌──────────────┐
│Auth Service │    │Patient Service│    │Other Services│
│  (4005)     │    │    (4000)     │    │              │
└─────────────┘    └──────┬────────┘    └──────────────┘
       │                  │
       ▼                  ├─────gRPC────▶┌──────────────┐
┌─────────────┐          │              │Billing Service│
│PostgreSQL   │          │              │  (4001/9001) │
│auth-service │          │              └──────────────┘
│    -db      │          │
└─────────────┘          │
                         ├─────Kafka───▶┌──────────────┐
                         │              │Analytics Svc │
                         │              │   (4002)     │
                         │              └──────────────┘
                         ▼
                  ┌─────────────┐
                  │PostgreSQL   │
                  │patient-svc  │
                  │    -db      │
                  └─────────────┘
```

## Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Primary programming language |
| Spring Boot | 3.4.1 / 4.0.0 | Microservices framework |
| Maven | 3.9.9 | Build automation and dependency management |
| PostgreSQL | 17.2 | Relational database |
| Apache Kafka | 3.7.x | Message broker for event streaming |
| gRPC | 1.69.0 | RPC framework for inter-service communication |
| Protocol Buffers | 3.25.5 | Data serialization |
| Docker | Latest | Containerization |

### Spring Framework Modules

- **Spring Cloud Gateway**: API Gateway implementation
- **Spring Data JPA**: Database access and ORM
- **Spring Security**: Authentication and authorization
- **Spring Kafka**: Kafka integration
- **Spring Web**: REST API development

### AWS Services

- **ECS Fargate**: Container orchestration (serverless)
- **RDS PostgreSQL**: Managed database service
- **MSK (Managed Streaming for Kafka)**: Managed Kafka cluster
- **Application Load Balancer**: Traffic distribution
- **VPC**: Network isolation
- **CloudWatch**: Logging and monitoring
- **AWS CDK**: Infrastructure as Code (v2.178.1)

### Security & Documentation

- **JJWT**: JSON Web Token implementation (v0.12.6)
- **SpringDoc OpenAPI**: API documentation (v2.6.0-2.7.0)
- **JUnit 5**: Unit testing
- **REST Assured**: API integration testing

## Microservices

### 1. Auth Service

**Port**: 4005
**Database**: PostgreSQL (auth-service-db)
**Responsibility**: User authentication and JWT token management

#### Features
- User login with email/password authentication
- JWT token generation with 10-hour expiration
- Token validation for protected endpoints
- Spring Security integration with BCrypt password encoding

#### API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/login` | Authenticate user and receive JWT token | No |
| GET | `/validate` | Validate Bearer token | Yes |

#### Data Model

```java
User {
    UUID id
    String email (unique)
    String password (encrypted)
    String role
    LocalDateTime createdAt
}
```

#### Configuration
- **JWT Secret**: Base64 encoded secret key
- **Token Expiration**: 36000000ms (10 hours)
- **Security**: CORS enabled, CSRF disabled for stateless API

---

### 2. Patient Service

**Port**: 4000
**Database**: PostgreSQL (patient-service-db)
**Responsibility**: Patient record management and coordination

#### Features
- Complete CRUD operations for patient records
- Automatic billing account creation via gRPC
- Event publishing to Kafka for analytics
- Email validation and uniqueness constraints
- UUID-based patient identification

#### API Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/patients` | Retrieve all patients | Yes (via Gateway) |
| POST | `/patients` | Create new patient | Yes (via Gateway) |
| PUT | `/patients/{id}` | Update existing patient | Yes (via Gateway) |
| DELETE | `/patients/{id}` | Delete patient | Yes (via Gateway) |

#### Data Model

```java
Patient {
    UUID id
    String name (required)
    String email (unique, validated)
    String address
    LocalDate dateOfBirth
    LocalDateTime registrationDate
}
```

#### Integration Points
- **gRPC Client**: Calls Billing Service on patient creation
- **Kafka Producer**: Publishes `PATIENT_CREATED` events to "patient" topic

---

### 3. Billing Service

**Port**: 4001 (HTTP), 9001 (gRPC)
**Responsibility**: Billing account management

#### Features
- gRPC-based service for high-performance RPC
- Synchronous billing account creation
- Protocol Buffers for strongly-typed contracts

#### gRPC Service Definition

```protobuf
service BillingService {
  rpc CreateBillingAccount (BillingRequest) returns (BillingResponse);
}

message BillingRequest {
  string patientId = 1;
  string name = 2;
  string email = 3;
}

message BillingResponse {
  string accountId = 1;
  string status = 2;
}
```

#### Technical Details
- **Protocol**: gRPC over HTTP/2
- **Serialization**: Protocol Buffers (protobuf)
- **Java Package**: `billing`

---

### 4. Analytics Service

**Port**: 4002
**Responsibility**: Event consumption and analytics processing

#### Features
- Asynchronous Kafka event consumption
- Patient event processing and logging
- Real-time analytics capabilities

#### Event Processing
- **Consumer Group**: `analytics-service`
- **Topic**: `patient`
- **Event Type**: `PatientEvent` (Protocol Buffers)
- **Processing**: Logs and analyzes patient creation events

#### Event Schema

```protobuf
message PatientEvent {
  string patientId = 1;
  string name = 2;
  string email = 3;
  string event_type = 4;  // e.g., "PATIENT_CREATED"
}
```

---

### 5. API Gateway

**Port**: 4004
**Technology**: Spring Cloud Gateway
**Responsibility**: Request routing and security filtering

#### Features
- Centralized request routing to microservices
- JWT token validation for protected routes
- API documentation aggregation
- Load balancing via Application Load Balancer

#### Routes Configuration

```yaml
/auth/**              → Auth Service (4005) - No JWT required
/api/patients/**      → Patient Service (4000) - JWT required
/api-docs/**          → Service documentation endpoints
```

#### Security Filters
- **JWT Validation Filter**: Validates Bearer tokens for `/api/**` routes
- **Header Format**: `Authorization: Bearer {jwt_token}`
- **Validation**: Calls Auth Service `/validate` endpoint

#### Environment Profiles
- **Default**: Docker network routing
- **Production** (`application-prod.yml`): localhost.docker.internal routing

---

## Infrastructure

The infrastructure is defined using **AWS CDK (Cloud Development Kit)** in Java, enabling version-controlled, reproducible infrastructure.

### Infrastructure Components

#### 1. Virtual Private Cloud (VPC)

```java
VPC: PatientManagementVPC
- Max Availability Zones: 2
- Private & Public Subnets
- NAT Gateways for outbound traffic
```

#### 2. Databases (RDS PostgreSQL)

**Auth Service Database**
- Name: `auth-service-db`
- Engine: PostgreSQL 17.2
- Instance: BURSTABLE2 MICRO
- Storage: 20 GB
- Credentials: Auto-generated secret

**Patient Service Database**
- Name: `patient-service-db`
- Engine: PostgreSQL 17.2
- Instance: BURSTABLE2 MICRO
- Storage: 20 GB
- Credentials: Auto-generated secret

**Health Checks**
- Type: TCP
- Interval: 30 seconds
- Failure Threshold: 3

#### 3. Message Broker (Amazon MSK)

```java
Cluster: kafka-cluster
- Kafka Version: 3.7.x
- Broker Nodes: 2
- Instance Type: kafka.m5.xlarge
- Distribution: DEFAULT (across AZs)
- Bootstrap Servers: localhost.localstack.cloud:4510-4512
```

#### 4. Container Orchestration (ECS Fargate)

**ECS Cluster**
- Name: `PatientManagementCluster`
- Service Discovery: CloudMap namespace (`patient-management.local`)

**Task Definitions**
- CPU: 256 (0.25 vCPU)
- Memory: 512 MiB
- Network Mode: awsvpc
- Launch Type: FARGATE (serverless)

**Fargate Services**
1. `auth-service` (Port 4005)
2. `patient-service` (Port 4000)
3. `billing-service` (Ports 4001, 9001)
4. `analytics-service` (Port 4002)
5. `api-gateway` (Port 4004 with ALB)

#### 5. Load Balancing

**Application Load Balancer**
- Service: API Gateway
- Health Check Grace Period: 60 seconds
- Target Type: IP (for Fargate)
- Public-facing with DNS

#### 6. Logging (CloudWatch)

- **Log Groups**: `/ecs/{service-name}`
- **Retention**: 1 day
- **Stream Prefix**: Service name
- **Log Driver**: awslogs

### Infrastructure Deployment

```bash
# Located in infastructure/
cd infastructure

# Synthesize CDK stack
mvn compile exec:java

# Deploy to AWS (when configured)
cdk deploy

# Output directory
./cdk.out/
```

## Getting Started

### Prerequisites

- **Java 21** (OpenJDK or Eclipse Temurin)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Git**
- **AWS CLI** (for cloud deployment)
- **Protocol Buffers Compiler** (protoc 3.25.5)

### Local Development Setup

#### 1. Clone the Repository

```bash
git clone <repository-url>
cd managment-system
```

#### 2. Build All Services

```bash
# Build individual services
cd auth-service && mvn clean install && cd ..
cd patient-service && mvn clean install && cd ..
cd billing-service && mvn clean install && cd ..
cd analytics-service && mvn clean install && cd ..
cd api-gateway && mvn clean install && cd ..
```

#### 3. Set Up Infrastructure

```bash
# Build infrastructure CDK project
cd infastructure
mvn clean install
mvn compile exec:java  # Synthesize CloudFormation
cd ..
```

#### 4. Start Local Services with Docker

Each service has a multi-stage Dockerfile:

```bash
# Build Docker images
docker build -t auth-service ./auth-service
docker build -t patient-service ./patient-service
docker build -t billing-service ./billing-service
docker build -t analytics-service ./analytics-service
docker build -t api-gateway ./api-gateway
```

#### 5. Configure Environment Variables

**Auth Service** (`auth-service/src/main/resources/application.properties`):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/auth-service-db
spring.datasource.username=admin_user
spring.datasource.password=<your-password>
jwt.secret=Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM=
jwt.expiration=36000000
```

**Patient Service**:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/patient-service-db
billing.service.address=host.docker.internal
billing.service.grpc.port=9001
spring.kafka.bootstrap-servers=localhost:9092
```

**Analytics Service**:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=analytics-service
```

#### 6. Run Services

```bash
# Terminal 1 - Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 2 - Billing Service
cd billing-service
mvn spring-boot:run

# Terminal 3 - Patient Service
cd patient-service
mvn spring-boot:run

# Terminal 4 - Analytics Service
cd analytics-service
mvn spring-boot:run

# Terminal 5 - API Gateway
cd api-gateway
mvn spring-boot:run
```

## API Documentation

### Swagger/OpenAPI Documentation

Once services are running, access the API documentation:

- **Auth Service**: http://localhost:4005/swagger-ui.html
- **Patient Service**: http://localhost:4000/swagger-ui.html
- **API Gateway**: http://localhost:4004/swagger-ui.html

### Example API Requests

#### Authentication

**Login** ([api-requests/auth-service/login.http](api-requests/auth-service/login.http))

```http
POST http://localhost:4004/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 36000000
}
```

---

**Validate Token** ([api-requests/auth-service/validate.http](api-requests/auth-service/validate.http))

```http
GET http://localhost:4004/auth/validate
Authorization: Bearer {your-jwt-token}
```

#### Patient Management

**Get All Patients** ([api-requests/patient-service/get-patients.http](api-requests/patient-service/get-patients.http))

```http
GET http://localhost:4004/api/patients
Authorization: Bearer {your-jwt-token}
```

**Create Patient** ([api-requests/patient-service/create-patient.http](api-requests/patient-service/create-patient.http))

```http
POST http://localhost:4004/api/patients
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "address": "123 Main St, City, State 12345",
  "dateOfBirth": "1990-05-15"
}
```

**Update Patient** ([api-requests/patient-service/update-patient.http](api-requests/patient-service/update-patient.http))

```http
PUT http://localhost:4004/api/patients/{patient-id}
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "name": "John Doe Updated",
  "email": "john.updated@example.com",
  "address": "456 New Address",
  "dateOfBirth": "1990-05-15"
}
```

**Delete Patient** ([api-requests/patient-service/delete-patient.http](api-requests/patient-service/delete-patient.http))

```http
DELETE http://localhost:4004/api/patients/{patient-id}
Authorization: Bearer {your-jwt-token}
```

#### Billing (gRPC)

**Create Billing Account** ([grpc-requests/billing-service/create-billing-account.http](grpc-requests/billing-service/create-billing-account.http))

```bash
# Using grpcurl
grpcurl -plaintext \
  -d '{"patientId": "uuid-here", "name": "John Doe", "email": "john@example.com"}' \
  localhost:9001 \
  BillingService/CreateBillingAccount
```

## Communication Patterns

### 1. Synchronous Communication (gRPC)

**Use Case**: Patient Service → Billing Service

- **Protocol**: gRPC over HTTP/2
- **Serialization**: Protocol Buffers
- **Characteristics**:
  - Low latency
  - Strongly typed contracts
  - Bidirectional streaming support
  - HTTP/2 multiplexing

**Flow**:
```
Patient Creation Request
    ↓
Patient Service validates data
    ↓
Patient Service saves patient to DB
    ↓
Patient Service calls Billing Service (gRPC)
    ↓
Billing Service creates billing account
    ↓
Billing Service returns accountId
    ↓
Patient Service returns response to client
```

### 2. Asynchronous Communication (Apache Kafka)

**Use Case**: Patient Service → Analytics Service

- **Message Broker**: Apache Kafka (MSK)
- **Topic**: `patient`
- **Serialization**: Protocol Buffers
- **Characteristics**:
  - Fire-and-forget messaging
  - Decoupled services
  - Event sourcing capability
  - Fault tolerance and replay

**Flow**:
```
Patient Created
    ↓
Patient Service publishes PatientEvent to Kafka
    ↓
Kafka stores event in "patient" topic
    ↓
Analytics Service consumes event (async)
    ↓
Analytics Service processes and logs event
```

### 3. REST over HTTP

**Use Case**: Client → API Gateway → Services

- **Protocol**: HTTP/REST
- **Format**: JSON
- **Authentication**: JWT Bearer tokens
- **Characteristics**:
  - Stateless
  - Cacheable
  - Standard HTTP methods

## Security

### Authentication Flow

1. **User Login**:
   - Client sends credentials to `/auth/login`
   - Auth Service validates credentials
   - Auth Service generates JWT token
   - Client receives token with 10-hour expiration

2. **Protected Resource Access**:
   - Client includes JWT in `Authorization: Bearer {token}` header
   - API Gateway intercepts request
   - Gateway calls Auth Service `/validate` endpoint
   - If valid, request is forwarded to target service
   - If invalid, 401 Unauthorized is returned

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "role": "USER",
  "iat": 1702345678,
  "exp": 1702381678
}
```

### Security Features

- **Password Encryption**: BCrypt hashing (Spring Security)
- **Token Expiration**: 10-hour automatic expiration
- **Stateless Authentication**: No server-side session storage
- **Database Security**: Auto-generated credentials, AWS Secrets Manager
- **Network Isolation**: VPC with private subnets
- **HTTPS**: Load Balancer SSL/TLS termination (production)

## Testing

### Integration Tests

Located in [integration-tests/](integration-tests/)

**Auth Service Tests** ([AuthIntegrationTest.java](integration-tests/src/test/java/AuthIntegrationTest.java))
```bash
cd integration-tests
mvn test -Dtest=AuthIntegrationTest
```

**Patient Service Tests** ([PatientIntegrationTest.java](integration-tests/src/test/java/PatientIntegrationTest.java))
```bash
mvn test -Dtest=PatientIntegrationTest
```

### Test Framework

- **JUnit 5**: Unit testing framework
- **REST Assured**: REST API testing
- **Spring Test**: Spring context testing
- **H2 Database**: In-memory database for testing

### Running All Tests

```bash
# Run all tests across all services
mvn clean verify

# Run tests for specific service
cd patient-service
mvn test
```

## Deployment

### AWS Deployment with CDK

1. **Configure AWS Credentials**
```bash
aws configure
```

2. **Bootstrap CDK** (first time only)
```bash
cd infastructure
cdk bootstrap
```

3. **Synthesize CloudFormation**
```bash
mvn compile exec:java
```

4. **Deploy Stack**
```bash
cdk deploy
```

5. **Verify Deployment**
```bash
aws ecs list-services --cluster PatientManagementCluster
aws rds describe-db-instances
```

### Docker Deployment

Each service uses multi-stage Docker builds for optimization:

**Build Stage** (Maven)
- Base: `maven:3.9.9-eclipse-temurin-21`
- Downloads dependencies
- Compiles source code
- Creates executable JAR

**Runtime Stage** (JDK)
- Base: `eclipse-temurin:21-jdk`
- Copies only JAR file
- Minimizes image size
- Optimized for production

### Environment-Specific Configuration

- **Development**: `application.properties`
- **Production**: `application-prod.yml` (Spring profile: `prod`)
- **Testing**: `application-test.properties`

## Development

### Code Organization

Each service follows a **layered architecture**:

```
service/
├── src/main/java/com/{service}/
│   ├── controller/       # REST/gRPC endpoints
│   ├── service/          # Business logic
│   ├── repository/       # Data access
│   ├── entity/           # JPA entities
│   ├── dto/              # Data transfer objects
│   ├── mapper/           # Entity-DTO mappers
│   ├── config/           # Configuration classes
│   ├── exception/        # Custom exceptions
│   └── util/             # Utility classes
├── src/main/proto/       # Protocol Buffer definitions
└── src/main/resources/
    ├── application.properties
    └── data.sql          # Initial data
```

### Design Patterns Used

- **API Gateway Pattern**: Centralized entry point
- **Database per Service**: Service isolation
- **Event Sourcing**: Kafka event publishing
- **Circuit Breaker**: Resilience4j (optional)
- **Service Discovery**: AWS CloudMap
- **DTO Pattern**: Request/Response objects
- **Repository Pattern**: Data access abstraction

### Adding a New Service

1. Create Maven module
2. Add Spring Boot dependencies
3. Define data model (entities, DTOs)
4. Implement business logic
5. Create Dockerfile
6. Add to CDK infrastructure
7. Configure service discovery
8. Update API Gateway routes (if needed)

### Protocol Buffers Development

**Compile Proto Files**:
```bash
cd patient-service
mvn protobuf:compile
mvn protobuf:compile-custom  # For gRPC
```

**Proto Files Location**:
- Source: `src/main/proto/`
- Generated: `target/generated-sources/protobuf/`

## Project Structure

```
managment-system/
├── analytics-service/         # Kafka event consumer service
│   ├── src/
│   │   ├── main/java/com/pm/analytics/
│   │   └── main/proto/       # patient_event.proto
│   ├── Dockerfile
│   └── pom.xml
│
├── api-gateway/              # Spring Cloud Gateway
│   ├── src/
│   │   ├── main/java/com/pm/gateway/
│   │   └── main/resources/
│   │       ├── application.yml
│   │       └── application-prod.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── auth-service/             # Authentication service
│   ├── src/
│   │   ├── main/java/com/pm/auth/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── util/
│   │   └── main/resources/
│   ├── Dockerfile
│   └── pom.xml
│
├── billing-service/          # gRPC billing service
│   ├── src/
│   │   ├── main/java/com/pm/billing/
│   │   └── main/proto/       # billing_service.proto
│   ├── Dockerfile
│   └── pom.xml
│
├── patient-service/          # Patient management service
│   ├── src/
│   │   ├── main/java/com/pm/patient/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── exception/
│   │   │   ├── config/
│   │   │   └── grpc/
│   │   └── main/proto/       # billing_service.proto, patient_event.proto
│   ├── Dockerfile
│   └── pom.xml
│
├── infastructure/            # AWS CDK infrastructure
│   ├── src/main/java/com/pm/stack/
│   │   └── LocalStack.java  # CDK stack definition
│   ├── cdk.out/             # Synthesized CloudFormation
│   └── pom.xml
│
├── integration-tests/        # End-to-end tests
│   ├── src/test/java/
│   │   ├── AuthIntegrationTest.java
│   │   └── PatientIntegrationTest.java
│   └── pom.xml
│
├── api-requests/             # HTTP request examples
│   ├── auth-service/
│   │   ├── login.http
│   │   └── validate.http
│   └── patient-service/
│       ├── create-patient.http
│       ├── get-patients.http
│       ├── update-patient.http
│       └── delete-patient.http
│
├── grpc-requests/            # gRPC request examples
│   └── billing-service/
│       └── create-billing-account.http
│
└── README.md
```

## Contributing

### Development Workflow

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make changes and commit**
   ```bash
   git add .
   git commit -m "Add feature: your feature description"
   ```
4. **Run tests**
   ```bash
   mvn clean verify
   ```
5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
6. **Create Pull Request**

### Code Standards

- **Java**: Follow Google Java Style Guide
- **Naming**: Descriptive names, camelCase for variables
- **Documentation**: JavaDoc for public APIs
- **Testing**: Minimum 70% code coverage
- **Commit Messages**: Conventional Commits format

### Git Workflow

**Recent Commits**:
```
f1bd488 - update/fix and test deployment
05b6ccf - preparing docker images
465c33e - create load balanced application gateway
c95cc16 - create ecs services
c545248 - create ecs cluster
```

**Main Branch**: `main`
**Development**: Feature branches → `main` via PR

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Contact & Support

For questions, issues, or contributions:
- **Issues**: [GitHub Issues](<repository-url>/issues)
- **Documentation**: This README and inline JavaDoc
- **API Docs**: Swagger UI (see [API Documentation](#api-documentation))

---

## Acknowledgments

Built with:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Apache Kafka](https://kafka.apache.org/)
- [gRPC](https://grpc.io/)
- [PostgreSQL](https://www.postgresql.org/)
- [AWS CDK](https://aws.amazon.com/cdk/)
- [Protocol Buffers](https://protobuf.dev/)

---

**Last Updated**: December 2025
**Project Status**: Active Development
