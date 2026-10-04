# RideLink — Microservices Ride-Sharing Platform
**Course:** IT3130 Application Development — University Group Assignment  
**Architecture:** Microservices Architecture (Spring Boot 3.2, Java 17+, Spring Cloud OpenFeign)  
**Security:** Spring Security 6 with JJWT & Role-Based Access Control (RBAC)  
**Database:** Database-Per-Service Pattern (Isolated H2 In-Memory Data Stores)

---

## 1. Executive Summary & Architecture Overview

**RideLink** is an enterprise-grade backend ride-sharing system engineered strictly using **Java 17+** and **Spring Boot 3.2.x**. It is structured as a decoupled multi-module microservice platform comprising **EXACTLY four core services**, each fulfilling a bounded context with zero shared databases.

```
                           +------------------------+
                           |  API Clients / Postman |
                           +-----------+------------+
                                       |
                   +-------------------+-------------------+
                   |                   |                   |
                   v                   v                   v
            [Port: 8081]        [Port: 8082]        [Port: 8084]
        +------------------+ +-----------------+ +-------------------+
        | Account Service  | | Driver Service  | |   Fare Service    |
        | (Auth, JWT, RBAC)| | (Fleet & Avail) | | (Fare, Pay, Rcpt) |
        +--------+---------+ +--------+--------+ +---------+---------+
                 |                    ^                    ^
                 | (H2: accountdb)    | (H2: driverdb)     | (H2: faredb)
                 |                    |                    |
                 |             OpenFeign (Sync)     OpenFeign (Sync)
                 |             (Driver Query)       (Fare Calculation)
                 |                    |                    |
                 |             +------+--------------------+
                 |             |
                 v             v
          [Port: 8083]  +----------------------+
          ------------> |     Ride Service     |
                        | (Lifecycle & State)  |
                        +----------+-----------+
                                   | (H2: ridedb)
```

---

## 2. Microservices Breakdown & Responsibilities

| Service | Port | Database | Primary Responsibilities |
| :--- | :---: | :---: | :--- |
| **Account Service** | `8081` | `jdbc:h2:mem:accountdb` | Passenger & Driver registration, BCrypt password hashing, JWT token issuance, RBAC (`PASSENGER`, `DRIVER`, `ADMIN`), profile management. |
| **Driver Service** | `8082` | `jdbc:h2:mem:driverdb` | Driver operational profiles, vehicle registration, availability toggle (`ONLINE`/`OFFLINE`), simulated GPS tracking, Haversine nearby driver search. |
| **Ride Service** | `8083` | `jdbc:h2:mem:ridedb` | Ride request creation, driver assignment via Feign, strict 6-state lifecycle state machine, ride history tracking. |
| **Fare Service** | `8084` | `jdbc:h2:mem:faredb` | Trip fare estimation, deterministic post-ride fare calculation, simulated payment gateway processing, automated digital receipt issuance. |

---

## 3. Inter-Service Communication & Workflows

Inter-service communication is designed synchronously using **Spring Cloud OpenFeign** with a custom **JWT Bearer RequestInterceptor** (`FeignConfig`), ensuring authentication credentials securely propagate between services without exposing credentials in client queries.

### Workflow 1: Ride Request & Driver Matching
1. Passenger sends `POST http://localhost:8083/api/rides` with pickup and destination GPS coordinates.
2. `ride-service` makes a synchronous OpenFeign call to `driver-service` (`GET /api/drivers/available?lat=...&lon=...&radiusKm=10.0`).
3. `driver-service` scans verified, `ONLINE` drivers, calculates spatial distance using the **Haversine formula**, and returns eligible drivers sorted by proximity.
4. `ride-service` matches the closest driver, links the driver ID, updates ride status to `ASSIGNED`, and returns the ride payload.

### Workflow 2: Ride Completion & Final Fare Calculation
1. Driver finishes the trip and sends `PATCH http://localhost:8083/api/rides/{id}/complete`.
2. `ride-service` validates state machine rules (`IN_PROGRESS` $\rightarrow$ `COMPLETED`).
3. `ride-service` triggers a synchronous OpenFeign call to `fare-service` (`POST /api/fares/calculate`).
4. `fare-service` executes the documented pricing rule:
   $$\text{Total Fare} = \Big(\text{Base Fare} + (\text{Distance km} \times \text{Per-Km Rate}) + (\text{Duration min} \times \text{Per-Min Rate})\Big) \times \text{Surge Multiplier}$$
5. The calculated fare is saved in `faredb` and mirrored onto the `Ride` record in `ridedb`.

### Workflow 3: Payment Processing & Receipt Generation
1. Passenger sends `POST http://localhost:8084/api/payments/process` with payment method and ride ID.
2. `fare-service` verifies the ride fare, validates the passenger's identity, and checks for duplicate payments.
3. The simulated payment gateway executes:
   - Success scenario generates a transaction reference (`TXN-XXXX`) and an immutable digital `Receipt` (`RCPT-YYYY-XXXX`).
   - Failure scenario (e.g. test cards ending in `0002`) marks transaction as `FAILED` and returns `422 Unprocessable Entity`.
4. Passenger can retrieve receipts anytime via `GET /api/receipts/ride/{rideId}`.

---

## 4. Ride Lifecycle State Machine

```
              [Create Ride]
                    |
                    v
             +--------------+
             |  REQUESTED   |
             +------+-------+
                    | (Driver Matched)
                    v
             +--------------+
             |   ASSIGNED   +----------------+
             +------+-------+                |
                    | (Driver Accepts)       |
                    v                        |
             +--------------+                | (Cancel anytime
             |   ACCEPTED   +----------------+  before COMPLETED)
             +------+-------+                |
                    | (Driver Starts Ride)   |
                    v                        |
             +--------------+                |
             | IN_PROGRESS  +----------------+
             +------+-------+                |
                    |                        |
                    | (Driver Completes)     |
                    v                        v
             +--------------+         +--------------+
             |  COMPLETED   |         |  CANCELLED   |
             +--------------+         +--------------+
```

Any illegal transition (e.g., trying to complete a ride that is still `REQUESTED`) is rejected with a descriptive `409 Conflict` error.

---

## 5. Security & Configuration Management

- **Zero Hardcoded Secrets**: All microservices read secrets via Spring Environment properties with fallback defaults:
  - `JWT_SECRET`: Base64 encoded 256-bit secret key.
  - `JWT_EXPIRATION_MS`: Token lifespan (default: 24 hours).
- **Stateless Authorization**: `JwtAuthFilter` validates the bearer token signature and injects `ROLE_PASSENGER`, `ROLE_DRIVER`, or `ROLE_ADMIN` into Spring's `SecurityContextHolder`.
- **Method-Level Security**: Controllers use `@PreAuthorize("hasRole('...')")` to guarantee strict authorization boundaries.

---

## 6. Prerequisites & Quick Start Guide

### Prerequisites
- **JDK 17** or higher (`java -version`)
- **Apache Maven 3.8+** (`mvn -v`)

### Port Allocation
Ensure ports `8081`, `8082`, `8083`, and `8084` are free on `localhost`.

### Build All Microservices
From the project root directory, run:
```bash
mvn clean install -DskipTests
```

### Run Unit Tests
To execute all JUnit 5 / Mockito unit tests across all 4 microservices:
```bash
mvn test
```

### Starting the Microservices
Start each microservice in separate terminal windows in the following recommended order:

**Terminal 1 — Account Service (Port 8081):**
```bash
cd account-service
mvn spring-boot:run
```

**Terminal 2 — Driver & Vehicle Service (Port 8082):**
```bash
cd driver-service
mvn spring-boot:run
```

**Terminal 3 — Fare & Payment Service (Port 8084):**
```bash
cd fare-service
mvn spring-boot:run
```

**Terminal 4 — Ride Management Service (Port 8083):**
```bash
cd ride-service
mvn spring-boot:run
```

---

## 7. Interactive API Documentation (Swagger UI)

When the microservices are running, access their interactive OpenAPI 3.0 documentation:

| Service | Swagger UI URL | OpenAPI JSON Spec |
| :--- | :--- | :--- |
| **Account Service** | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| **Driver Service** | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |
| **Ride Service** | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) |
| **Fare Service** | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs) |

### Embedded Database Consoles (H2)
Each service provides an isolated in-memory H2 database console:
- Account Service: `http://localhost:8081/h2-console` (JDBC URL: `jdbc:h2:mem:accountdb`, User: `sa`, Password: empty)
- Driver Service: `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:mem:driverdb`, User: `sa`, Password: empty)
- Ride Service: `http://localhost:8083/h2-console` (JDBC URL: `jdbc:h2:mem:ridedb`, User: `sa`, Password: empty)
- Fare Service: `http://localhost:8084/h2-console` (JDBC URL: `jdbc:h2:mem:faredb`, User: `sa`, Password: empty)

---

## 8. Postman Testing Guide

A complete, pre-configured collection is provided at:  
`postman/RideLink_Microservices.postman_collection.json`

### Import Instructions
1. Open Postman $\rightarrow$ Click **Import** $\rightarrow$ Select `postman/RideLink_Microservices.postman_collection.json`.
2. The collection contains automated test scripts that automatically capture and chain JWT tokens:
   - Running `1.2 Login Passenger` extracts `passenger_token` and `passenger_id`.
   - Running `1.4 Login Driver` extracts `driver_token` and `driver_id`.
   - Running `3.1 Request a Ride` stores `ride_id`.
   - Running `4.3 Process Payment` stores `receipt_number`.

### Test Scenarios Covered
- **Happy Paths**:
  1. Register & login passenger.
  2. Register & login driver.
  3. Register vehicle & verify driver operational profile.
  4. Driver toggles availability to `ONLINE` and sets location.
  5. Passenger requests ride $\rightarrow$ Driver is automatically matched via Feign.
  6. Driver accepts, starts, and completes ride $\rightarrow$ Fare is automatically calculated via Feign.
  7. Passenger pays for ride $\rightarrow$ Digital receipt is issued.
- **Negative & Edge Cases**:
  1. Invalid login credentials (`401 Unauthorized`).
  2. Duplicate email registration (`409 Conflict`).
  3. Unauthorized role access (Passenger attempting driver location update $\rightarrow$ `403 Forbidden`).
  4. Invalid state machine transitions (Attempting to complete an unstarted ride $\rightarrow$ `409 Conflict`).
  5. Simulated credit card decline (Card ending in `0002` $\rightarrow$ `422 Unprocessable Entity`).
  6. Duplicate payment attempt on already paid ride (`409 Conflict`).

---

## 9. Continuous Integration (CI) Pipeline

A production GitHub Actions workflow is pre-configured in `.github/workflows/ci.yml`.  
It automatically triggers on pushes and pull requests to `main`, setting up JDK 17, compiling all microservices, running JUnit 5 tests, and generating test result summaries.
