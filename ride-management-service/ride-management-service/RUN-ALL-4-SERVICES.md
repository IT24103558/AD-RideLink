# Running all 4 RideLink services together (Swagger demo)

Use **4 terminals**, same `JWT_SECRET` in all of them, one Atlas cluster, **a different database name per service**:

| Service | Port | Database in `MONGODB_URI` |
|---|---|---|
| account-service | 8081 | `ridelink_account` |
| driver-vehicle-service | 8082 | `driverdb` |
| ride-management-service | 8083 | `ridelink_ride` |
| fare-payment-service | 8084 | `ridelink_fare_payment` |

Each terminal (cmd): `set "MONGODB_URI=..."`, `set "JWT_SECRET=..."`, then `.\mvnw.cmd spring-boot:run`.
Atlas → Network Access must allow your IP.

## Demo flow

1. **Account (8081)**: register + login a PASSENGER and a DRIVER → copy each `token` and the driver's account `id`.
2. **Driver (8082)** with the DRIVER token: `POST /api/v1/drivers` using `userId` = the driver's **account id**, e.g.
   `{"userId":"<driver account id>","fullName":"Kamal Perera","licenseNumber":"B1234567","phoneNumber":"0771234567","serviceArea":"Colombo","vehicle":{"plateNumber":"CAB1234","vehicleType":"CAR","model":"Prius","capacity":4}}`
   then `PATCH /api/v1/drivers/{id}/availability` `{"status":"AVAILABLE"}` and `PATCH .../location` `{"latitude":6.93,"longitude":79.86}`.
3. **Ride (8083)** with the PASSENGER token: `POST /api/v1/rides` → status `ASSIGNED`, driver + `estimatedFare` filled in.
4. **Ride (8083)** with the DRIVER token: `PATCH /api/v1/rides/{id}/accept` → `/start` → `/complete` → `finalFare`, `fareStatus: CALCULATED`.
5. **Fare (8084)** with the PASSENGER token: `GET /api/v1/fares/ride/{rideId}` → `POST /api/v1/payments` `{"rideId":"<id>","method":"CARD"}` → `GET /api/v1/receipts/ride/{rideId}`.
   Try `"simulateFailure": true` to see a `402` / `FAILED` payment.
