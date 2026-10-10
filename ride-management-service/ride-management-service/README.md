# Ride Management Service — RideLink

IT3130 group project · **Member 3** · port **8083** · MongoDB Atlas database `ridelink_ride`

Responsibilities: ride request creation · pickup/destination details · driver assignment · ride status lifecycle ·
acceptance / start / completion / cancellation · ride retrieval.

## How it connects to the other services

| Service | Port | How |
|---|---|---|
| Account Service | 8081 | Issues the JWT. This service only **validates** it (same `JWT_SECRET`). `sub` = account id (stored as `passengerId` / `driverUserId`). |
| Driver & Vehicle Service | 8082 | `GET /api/v1/drivers/available` to pick a driver; `PATCH /api/v1/drivers/{id}/availability` → `BUSY` on start, `AVAILABLE` on complete (best effort, with the driver's own token). |
| Fare & Payment Service | 8084 | `POST /api/v1/fares/estimate` on request, `POST /api/v1/fares/final` on completion. |

The caller's JWT is **forwarded** to the downstream service (no separate service credentials). No other service's database is read;
driver/vehicle details are stored on the ride as a snapshot at assignment time.

## Ride lifecycle

```
REQUESTED ──(driver assigned)──► ASSIGNED ──accept──► ACCEPTED ──start──► IN_PROGRESS ──complete──► COMPLETED
    ▲                               │ reject
    └───────────────────────────────┘
CANCELLED  ← possible from REQUESTED, ASSIGNED, ACCEPTED (not once IN_PROGRESS)
```

Driver assignment: on `POST /rides` the service asks the Driver Service for AVAILABLE drivers, drops those who already have an
ASSIGNED/ACCEPTED/IN_PROGRESS ride, those who rejected this ride, and (if `vehicleType` was requested) other vehicle types,
then picks the **nearest to the pickup** (drivers without a location come last). If nobody is free the ride stays `REQUESTED`
and can be retried with `POST /rides/{id}/assign`. If the Fare Service is down, the ride still completes and `fareStatus` stays
`PENDING` until `POST /rides/{id}/fare` is called. A passenger can only have one active ride at a time.

## Configuration (environment variables)

| Variable | Purpose | Default (dev only) |
|---|---|---|
| `SERVER_PORT` | Port | `8083` |
| `MONGODB_URI` | MongoDB Atlas connection string — **must contain the db name** `/ridelink_ride` before the `?` | `mongodb://localhost:27017/ridelink_ride` |
| `JWT_SECRET` | Must be identical in all 4 services (≥ 32 chars) | `dev-only-secret-change-me-please-1234567890` |
| `DRIVER_SERVICE_URL` | Driver & Vehicle Service | `http://localhost:8082` |
| `FARE_SERVICE_URL` | Fare & Payment Service | `http://localhost:8084` |
| `SERVICES_TIMEOUT_MS` | Timeout for calls to other services | `5000` |

Never commit a real connection string or password.

## Run (Windows cmd, from this folder)

```bat
set "MONGODB_URI=mongodb+srv://USER:PASSWORD@YOURCLUSTER.mongodb.net/ridelink_ride?retryWrites=true&w=majority"
set "JWT_SECRET=dev-only-secret-change-me-please-1234567890"
.\mvnw.cmd spring-boot:run
```

Swagger UI: http://localhost:8083/swagger-ui.html (click **Authorize**, paste the token only — without `Bearer`).
Run tests: `.\mvnw.cmd test`

## Endpoints

| Method | Path | Who | Description |
|---|---|---|---|
| POST | `/api/v1/rides` | PASSENGER | Request a ride (auto-assigns a driver) → 201 |
| POST | `/api/v1/rides/{id}/assign` | PASSENGER of the ride / ADMIN | Retry assignment (404 `NO_DRIVER_AVAILABLE`) |
| PATCH | `/api/v1/rides/{id}/accept` | assigned DRIVER | ASSIGNED → ACCEPTED |
| PATCH | `/api/v1/rides/{id}/reject` | assigned DRIVER | ASSIGNED → REQUESTED, another driver is looked for |
| PATCH | `/api/v1/rides/{id}/start` | assigned DRIVER | ACCEPTED → IN_PROGRESS (driver → BUSY) |
| PATCH | `/api/v1/rides/{id}/complete` | assigned DRIVER | IN_PROGRESS → COMPLETED, final fare calculated (driver → AVAILABLE). Optional body |
| POST | `/api/v1/rides/{id}/fare` | assigned DRIVER / ADMIN | Re-send a COMPLETED ride to the Fare Service if `fareStatus` = PENDING |
| PATCH | `/api/v1/rides/{id}/cancel` | passenger / assigned driver / ADMIN | Cancel (optional `{"reason": "..."}`) |
| GET | `/api/v1/rides/me?status=` | any | My rides (passenger: requested · driver: assigned · admin: all) |
| GET | `/api/v1/rides?status=` | ADMIN | All rides |
| GET | `/api/v1/rides/{id}` | passenger / driver of the ride / ADMIN | One ride |

Sample bodies

```json
// POST /api/v1/rides   (vehicleType and serviceArea are optional)
{ "pickup":      { "address": "Colombo Fort",   "latitude": 6.9271, "longitude": 79.8612 },
  "destination": { "address": "Mount Lavinia",  "latitude": 6.8389, "longitude": 79.8653 },
  "vehicleType": "CAR", "serviceArea": "Colombo" }

// PATCH /api/v1/rides/{id}/complete   (optional; defaults to the estimates)
{ "actualDistanceKm": 12.5, "actualDurationMinutes": 27 }
```

## Errors

Same format as the other services: `{ "errorCode", "message", "timestamp", "details" }`.
Codes: `VALIDATION_ERROR` 400 · `INVALID_ROUTE` 400 · `UNAUTHORIZED` 401 · `ACCESS_DENIED` 403 · `RIDE_NOT_FOUND` 404 ·
`NO_DRIVER_AVAILABLE` 404 · `INVALID_STATE_TRANSITION` 409 · `ACTIVE_RIDE_EXISTS` 409 · `CONCURRENT_UPDATE` 409 ·
`DRIVER_SERVICE_UNAVAILABLE` 503 · `FARE_SERVICE_UNAVAILABLE` 503 · `DRIVER_SERVICE_ERROR` / `FARE_SERVICE_ERROR` 502 · `INTERNAL_ERROR` 500.

## Git / CI

`.github/workflows/ci.yml` runs `mvn clean verify` (JDK 17). If this service lives in the group's mono-repo, keep the folder name
`ride-management-service` (the workflow uses it as `working-directory`).
