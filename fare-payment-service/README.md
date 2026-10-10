# RideLink - Fare & Payment Service (Member 4)

[![Build & Test](https://img.shields.io/badge/Build-Passing-brightgreen)]()
[![Java](https://img.shields.io/badge/Java-17-blue)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen)]()
[![MongoDB](https://img.shields.io/badge/Database-MongoDB-green)]()

Backend microservice responsible for fare calculation, simulated payment transactions, and electronic receipt generation in the **RideLink** ride-sharing platform (IT3130 Application Development).

---

## 1. Service Responsibilities

- **Fare Estimation (`POST /api/fares/estimate` & `/api/v1/fares/estimate`)**:
  Estimates the fare for a trip using distance (km) or pickup/destination GPS coordinates (Haversine distance).
- **Final Fare Calculation (`POST /api/fares/final` & `/api/v1/fares/final`)**:
  Calculates and stores the authoritative completed trip fare into MongoDB.
- **Fare Retrieval (`GET /api/fares/{fareId}` & `GET /api/fares/ride/{rideId}`)**:
  Retrieves calculated fare records by MongoDB ID or stable ride ID.
- **Simulated Payment Processing (`POST /api/payments` & `/api/v1/payments`)**:
  Processes simulated payments (`CASH`, `CARD_SIMULATED`, `CARD`). Enforces idempotency (prevents duplicate payment for the same ride) and supports documented simulated failure testing (`simulateFailure: true` -> HTTP 402).
- **Payment Retrieval (`GET /api/payments/{paymentId}` & `GET /api/payments/ride/{rideId}`)**:
  Retrieves payment status, transaction reference, amount, and timestamp.
- **Receipt Issuance & Retrieval (`GET /api/payments/{paymentId}/receipt` & `GET /api/receipts/ride/{rideId}`)**:
  Issues and retrieves official electronic receipts **only for successful payments**. Rejects receipt generation for failed payment records.

---

## 2. Pricing Formula

The service uses a configurable formula:

$$\text{Fare} = \left(\text{Base Fare} + (\text{Distance in km} \times \text{Rate per km}) + (\text{Duration in minutes} \times \text{Rate per min})\right) \times \text{Vehicle Multiplier}$$

### Configurable Defaults:
- **Base Fare**: `200.00 LKR`
- **Rate per km**: `100.00 LKR/km`
- **Rate per minute**: `10.00 LKR/min` (applied when duration is present)
- **Currency**: `LKR`
- **Vehicle Multipliers**:
  - `CAR`: $1.0\times$
  - `BIKE`: $0.8\times$
  - `VAN`: $1.5\times$

---

## 3. Technology Stack

- **Java**: Java 17 (or Java 21 LTS)
- **Framework**: Spring Boot 3.2.5
- **Web**: Spring Boot Starter Web (REST APIs)
- **Persistence**: Spring Data MongoDB
- **Validation**: Jakarta Bean Validation (`@Valid`, `@NotNull`, `@DecimalMin`, `@NotBlank`)
- **API Documentation**: Springdoc OpenAPI 2.5.0 (Swagger UI 3.0)
- **Security**: Spring Security + JJWT 0.12.5 (HS256 stateless Bearer token validation)
- **Testing**: JUnit 5, Mockito, AssertJ, Spring MockMvc

---

## 4. Prerequisites

1. **JDK 17 or 21**: Verify with `java -version`.
2. **Maven 3.9+** (or use the included `./mvnw.cmd` / `./mvnw` wrapper).
3. **MongoDB**: Local MongoDB instance (`mongodb://localhost:27017`) or MongoDB Atlas cluster URI.

---

## 5. Configuration & Environment Variables

All sensitive values are externalized via environment variables.

| Environment Variable | Description | Default / Local Fallback |
|---|---|---|
| `SERVER_PORT` | HTTP port for this microservice | `8084` |
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017/ridelink_fare_payment` |
| `JWT_SECRET` | Secret key shared with Account Service | `dev-only-secret-change-me-please-1234567890` |
| `FARE_BASE_FARE` | Base fare charge | `200.00` |
| `FARE_RATE_PER_KM` | Rate charged per kilometre | `100.00` |
| `FARE_CURRENCY` | System currency code | `LKR` |

### Setting Environment Variables Locally

#### Windows (PowerShell):
```powershell
$env:MONGODB_URI = "mongodb+srv://<username>:<password>@cluster0.mongodb.net/ridelink_fare_payment?retryWrites=true&w=majority"
$env:JWT_SECRET = "dev-only-secret-change-me-please-1234567890"
$env:SERVER_PORT = "8084"
```

#### Windows (Command Prompt):
```cmd
set "MONGODB_URI=mongodb+srv://<username>:<password>@cluster0.mongodb.net/ridelink_fare_payment?retryWrites=true&w=majority"
set "JWT_SECRET=dev-only-secret-change-me-please-1234567890"
set "SERVER_PORT=8084"
```

---

## 6. Building and Running

### Run Unit Tests:
```powershell
.\mvnw.cmd clean test
```

### Run the Application:
```powershell
.\mvnw.cmd spring-boot:run
```

Once started, the service listens on **`http://localhost:8084`**.

---

## 7. Swagger UI / API Documentation

Swagger UI is interactively available at:
- **`http://localhost:8084/swagger-ui.html`** or **`http://localhost:8084/swagger-ui/index.html`**
- OpenAPI OpenAPI JSON definition: **`http://localhost:8084/v3/api-docs`**

To authenticate in Swagger UI:
1. Call `GET http://localhost:8084/api/auth/dev-token?role=PASSENGER` to generate a valid test token.
2. Click the **Authorize** button at the top right of Swagger UI.
3. Paste the token into the value box and click **Authorize**.

---

## 8. REST API Endpoints

| Method | Endpoint | Description | Role / Auth |
|---|---|---|---|
| `GET` | `/api/auth/dev-token` | Helper to generate signed mock JWT tokens for testing | Public |
| `POST` | `/api/fares/estimate` | Estimate ride fare based on distance or GPS coordinates | Public / Authenticated |
| `POST` | `/api/fares/final` | Calculate and persist final completed trip fare | Authenticated (DRIVER / ADMIN) |
| `GET` | `/api/fares/{fareId}` | Retrieve fare document by MongoDB ID | Authenticated |
| `GET` | `/api/fares/ride/{rideId}` | Retrieve fare document by stable ride ID | Authenticated |
| `POST` | `/api/payments` | Process simulated payment (CASH, CARD_SIMULATED) | Authenticated (PASSENGER) |
| `GET` | `/api/payments/{paymentId}` | Retrieve payment transaction by ID | Authenticated |
| `GET` | `/api/payments/ride/{rideId}` | Retrieve payment transaction by ride ID | Authenticated |
| `GET` | `/api/payments/{paymentId}/receipt` | Issue or retrieve official receipt for payment | Authenticated |
| `GET` | `/api/receipts/ride/{rideId}` | Retrieve official receipt for completed ride | Authenticated |

---

## 9. Testing with Postman

Postman collection and environment files are located in the `postman/` directory:
- `postman/RideLink_Fare_Payment_Service.postman_collection.json`
- `postman/RideLink_Local_Environment.postman_environment.json`

### Execution Order:
1. **Import** both files into Postman.
2. Select the **RideLink Local Environment**.
3. Run request **00. Auth -> 01. Generate PASSENGER JWT Token** (automatically sets `{{jwtToken}}` environment variable).
4. Run **01. Fare Estimation** requests.
5. Run **02. Final Fare Calculation** (automatically captures `{{fareId}}`).
6. Run **03. Fare Retrieval**.
7. Run **04. Payment Processing** (automatically captures `{{paymentId}}`).
8. Run **05. Payment Retrieval**.
9. Run **06. Receipt Generation & Retrieval**.

---

## 10. Interservice Integration

The Fare & Payment Service collaborates across the RideLink platform:
1. **Member 1 (Account Service)**:
   - Shared JWT secret for HS256 authentication.
   - User identity (`passengerId`, `driverUserId`) references Account Service credentials.
2. **Member 2 (Driver & Vehicle Service)**:
   - Consumes driver user ID and vehicle type (`CAR`, `BIKE`, `VAN`) to apply corresponding pricing multipliers.
3. **Member 3 (Ride Management Service)**:
   - When a ride is requested, Member 3 calls `POST /api/v1/fares/estimate` to display price estimates.
   - When a ride is completed, Member 3 calls `POST /api/v1/fares/final` to calculate final payable charges.
   - Passengers process payments and fetch receipts referencing the stable `rideId`.
