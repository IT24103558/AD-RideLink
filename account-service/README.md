# Account Service — RideLink

Part of the RideLink microservices backend (IT3130 Group Assignment).
**Primary owner: Member 1**

## Responsibility

- Passenger and driver **account registration**
- **Login and token (JWT) issuance** — the token is used by every other RideLink service
- **Role management** (PASSENGER / DRIVER / ADMIN)
- **Profile** viewing and updating, password change
- **Account status** management (ACTIVE / SUSPENDED / DEACTIVATED — only ACTIVE accounts can log in)

## Tech Stack

- Java 17, Spring Boot 3.2.5 (Web, Data MongoDB, Security, Validation)
- MongoDB Atlas — this service owns the `ridelink_account` database exclusively
- JWT (jjwt 0.12.5, HS256), BCrypt password hashing
- springdoc-openapi (Swagger UI), JUnit 5 + Mockito

## Prerequisites

- Java 17+ (Java 21 works)
- A MongoDB Atlas cluster (free M0 is enough). No Maven install needed — use `mvnw` / `mvnw.cmd`.

## Configuration (environment variables)

| Variable | Purpose | Default (dev only) |
|---|---|---|
| `MONGODB_URI` | Atlas connection string, **database name `ridelink_account`** | `mongodb://localhost:27017/ridelink_account` |
| `JWT_SECRET` | Signing secret, **min 32 characters, identical in Account, Driver & Vehicle and Ride Management** | `dev-only-secret-change-me-please-1234567890` |
| `JWT_EXPIRATION_MS` | Token lifetime | `3600000` (1 hour) |
| `SERVER_PORT` | Port | `8081` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional: creates the first ADMIN at start-up (password ≥ 8 chars) | not set |

Never commit real values. The default `JWT_SECRET` equals the Driver & Vehicle Service default, so the two services work together out of the box in development; set a real shared secret for demos.

## Running (Windows Command Prompt)

```
cd account-service
set "JWT_SECRET=<same-secret-as-the-other-services>"
set "MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/ridelink_account?retryWrites=true&w=majority"
set "ADMIN_EMAIL=admin@ridelink.test"
set "ADMIN_PASSWORD=Admin12345"
.\mvnw.cmd spring-boot:run
```

(Quotes matter because of the `&` characters. macOS/Linux: `export VAR=value` and `./mvnw spring-boot:run`.)

Swagger UI: http://localhost:8081/swagger-ui.html — OpenAPI JSON: http://localhost:8081/v3/api-docs

Atlas note: the database user needs read/write on `ridelink_account` (the default "read and write to any database" is fine). The `ridelink_account` database and `accounts` collection are created automatically on first write.

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/accounts/register` | public | Register a `PASSENGER` or `DRIVER` account (ADMIN cannot be self-registered) → `201` |
| POST | `/api/accounts/login` | public | Returns `token`, `tokenType`, `expiresInMs`, `id`, `name`, `email`, `role` |
| GET | `/api/accounts/me` | any logged-in user | Own profile |
| GET | `/api/accounts/{id}` | owner or ADMIN | Account by id |
| PUT | `/api/accounts/{id}` | owner or ADMIN | Update `name` / `phoneNumber` (optional fields) |
| PATCH | `/api/accounts/{id}/password` | owner only | Change password (`currentPassword`, `newPassword`) → `204` |
| GET | `/api/accounts?role=DRIVER` | ADMIN | List accounts, optional role filter |
| PATCH | `/api/accounts/{id}/role` | ADMIN | Change role |
| PATCH | `/api/accounts/{id}/status` | ADMIN | `ACTIVE` / `SUSPENDED` / `DEACTIVATED` |
| DELETE | `/api/accounts/{id}` | ADMIN | Delete account → `204` |

### Sample requests

Register a driver:
```json
{ "name": "Kasun Perera", "email": "kasun@example.com", "password": "Passw0rd123", "phoneNumber": "+94771234567", "role": "DRIVER" }
```
Login:
```json
{ "email": "kasun@example.com", "password": "Passw0rd123" }
```
Use `token` in Swagger's **Authorize** button (paste the token only, without `Bearer`) or as `Authorization: Bearer <token>`.

### Error format (same in every RideLink service)

```json
{ "errorCode": "DUPLICATE_EMAIL", "message": "...", "timestamp": "2026-10-08T10:00:00", "details": null }
```
Codes: `VALIDATION_ERROR` (400, `details` lists the fields), `INVALID_REQUEST_BODY` (400), `INVALID_ROLE` (400), `UNAUTHORIZED` (401), `INVALID_CREDENTIALS` (401), `ACCESS_DENIED` (403), `ACCOUNT_NOT_ACTIVE` (403), `ACCOUNT_NOT_FOUND` (404), `DUPLICATE_EMAIL` (409), `INTERNAL_ERROR` (500).

## Token contract with the other services

| Item | Value |
|---|---|
| Algorithm / key | HS256, key = UTF-8 bytes of `JWT_SECRET` |
| `sub` | the **account id** (stable identifier) |
| `role` | `PASSENGER`, `DRIVER` or `ADMIN` (other services turn it into `ROLE_<role>`) |
| `email`, `name` | extra convenience claims |
| `exp` | now + `JWT_EXPIRATION_MS` |

No service calls this service over HTTP to check a token — they verify the signature locally with the shared secret. A suspended account's *existing* token stays valid until it expires (stateless JWT, default 1 hour); new logins are blocked immediately.

## Connecting with Driver & Vehicle Service

1. Run both services with the **same `JWT_SECRET`** (or leave it unset in both).
2. Register a driver account here (`role: DRIVER`) and log in. Note the `id` and `token` from the login response.
3. In Driver & Vehicle Service (`http://localhost:8082/swagger-ui.html`), **Authorize** with that token, then `POST /api/v1/drivers` with **`userId` = the account `id`** from step 2.
4. Passenger accounts use their own token for passenger operations (e.g. creating rides).

`userId` in the driver profile is the account id (= the token's `sub`), so the driver's `/api/drivers/me/...` operations and Ride Management's driver matching use the same identifier everywhere.

## Running tests

```
.\mvnw.cmd test
```

Unit tests (Mockito, no database): registration (hashing, email normalisation, duplicate, ADMIN rejected), login (success, wrong password, unknown email, suspended account), profile update, password change, admin operations, and JWT tests including a contract test that verifies an issued token exactly the way Driver & Vehicle Service does.

## Notes for the Group

- This service exclusively owns the `ridelink_account` database; no other service reads or writes it.
- Other services store only the account `id`.
- Passwords are stored as BCrypt hashes and are never returned by any endpoint.
