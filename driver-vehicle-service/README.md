# Driver & Vehicle Service — RideLink

Part of the RideLink microservices backend (IT3130 Group Assignment).
**Primary owner: Member 2**

## Responsibility

- Driver operational profile registration and retrieval
- Vehicle details management
- Availability status (`AVAILABLE`, `BUSY`, `OFFLINE`)
- Simulated current location / service area
- Providing eligible **available drivers** to the Ride Management Service

## Tech Stack

- Java 17
- Spring Boot 3.2.5 (Web, Data MongoDB, Security, Validation)
- MongoDB (own database — no other service accesses it directly)
- springdoc-openapi (Swagger UI)
- JWT (validates tokens issued by the Account Service)
- JUnit 5 + Mockito (unit tests)

## Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB running locally (or a connection string to a hosted instance)

## Configuration (environment variables)

| Variable       | Purpose                                   | Default (dev only)                          |
|----------------|--------------------------------------------|----------------------------------------------|
| `SERVER_PORT`  | Port this service runs on                  | `8082`                                        |
| `MONGODB_URI`  | MongoDB connection string                  | `mongodb://localhost:27017/driverdb`          |
| `JWT_SECRET`   | Shared secret used to validate JWTs issued by the Account Service | dev placeholder — **must be overridden** and shared with the Account Service owner |

Never commit real secrets. Set these as environment variables or in a `.env` file that is git-ignored.

## Running the service

```bash
cd driver-vehicle-service
mvn spring-boot:run
```

The service starts on `http://localhost:8082` (or your configured `SERVER_PORT`).

## API Documentation

Once running:

- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Endpoints

| Method | Path                              | Auth              | Description                                  |
|--------|------------------------------------|-------------------|-----------------------------------------------|
| POST   | `/api/v1/drivers`                  | ROLE_DRIVER       | Register driver + vehicle                     |
| GET    | `/api/v1/drivers/{id}`             | Authenticated     | Get driver profile by driver id                |
| GET    | `/api/v1/drivers/user/{userId}`    | Authenticated     | Get driver profile by Account Service userId  |
| PUT    | `/api/v1/drivers/{id}/vehicle`     | ROLE_DRIVER       | Update vehicle details                        |
| PATCH  | `/api/v1/drivers/{id}/availability`| ROLE_DRIVER       | Update availability status                     |
| PATCH  | `/api/v1/drivers/{id}/location`    | ROLE_DRIVER       | Update simulated current location              |
| GET    | `/api/v1/drivers/available`        | Authenticated     | **Interservice contract** — used by Ride Management Service to fetch eligible drivers (optional `?serviceArea=`) |

All requests except Swagger routes require an `Authorization: Bearer <token>` header, where the token is issued by the Account Service.

## Interservice Contract

`GET /api/v1/drivers/available?serviceArea=Colombo` returns a JSON array of `DriverResponseDTO`:

```json
[
  {
    "id": "driver-001",
    "userId": "user-001",
    "fullName": "Kasun Perera",
    "licenseNumber": "B1234567",
    "phoneNumber": "+94771234567",
    "vehicle": { "plateNumber": "CAR-1234", "vehicleType": "CAR", "model": "Toyota Prius", "capacity": 4 },
    "availabilityStatus": "AVAILABLE",
    "serviceArea": "Colombo",
    "currentLocation": { "latitude": 6.9271, "longitude": 79.8612 },
    "createdAt": "2026-09-26T10:00:00",
    "updatedAt": "2026-09-26T10:05:00"
  }
]
```

If no driver is available, the service responds `404 Not Found` with error code `NO_DRIVER_AVAILABLE` — this is the required "no available driver" negative scenario.

**Share this contract with the Ride Management Service owner** so field names stay stable.

## Error Response Format

```json
{
  "errorCode": "DRIVER_NOT_FOUND",
  "message": "Driver not found with id: xyz",
  "timestamp": "2026-09-26T10:00:00",
  "details": null
}
```

Agree this format with the rest of the group so all four services return errors consistently.

## Running Tests

```bash
mvn test
```

Unit tests cover: successful registration, duplicate license/userId rejection, driver-not-found, availability update, and the available-drivers query (including the no-driver-found negative case).

## Sample Test Data

| Field         | Sample value      |
|---------------|--------------------|
| userId        | user-001           |
| licenseNumber | B1234567           |
| phoneNumber   | +94771234567       |
| serviceArea   | Colombo            |
| vehicleType   | CAR                |

## CI

A GitHub Actions workflow (`.github/workflows/ci.yml`) builds and runs tests on every push/PR touching this service's folder. Adjust the path if you place this folder differently inside the shared group repository.

## Notes for the Group

- This service owns the `driverdb` MongoDB database exclusively. No other service should query it directly.
- `userId` stored here is a stable reference to the Account Service's user record — not a foreign key into another database.
- JWT secret must match the Account Service's signing key.
