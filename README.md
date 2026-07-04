# RideShare — Full Web Application (Uber-like Ride Booking System)

A complete Spring Boot + MySQL web application: rider/driver registration &
login, destination selection on an interactive map, ride booking, real-time
driver location tracking, and a mock payment/checkout flow — plus the
underlying REST API.

## Tech Stack
- Java 17
- Spring Boot 3.3.4 (Web, Thymeleaf, Data JPA, Validation, WebSocket)
- MySQL 8
- Leaflet.js + OpenStreetMap (maps — free, no API key required)
- SockJS + STOMP.js (real-time updates over WebSocket)
- Maven

## What's included
- **Register/Login** as a Rider or Driver (session-based, BCrypt-hashed passwords)
- **Book a ride**: click the map to set pickup & destination (with reverse-geocoded
  address preview via OpenStreetMap Nominatim), pick a vehicle type
- **Automatic driver matching**: nearest available driver assigned instantly
- **Live tracking**: rider sees the driver's car icon move on the map in real time
  (WebSocket), driver dashboard shows incoming ride offers instantly
- **Full ride lifecycle**: request to assign to accept/reject to start to complete to pay
- **Mock payment/checkout**: choose Card/UPI/Wallet/Cash, get a fake transaction
  reference and a receipt page - no real gateway or card data involved
- **Ride history** for both riders and drivers
- The original REST API (`/api/**`) still works standalone for Postman/curl testing

## 1. Prerequisites
- JDK 17+ installed
- MySQL Server running locally (or update `application.properties` to point elsewhere)
- Eclipse with **m2e** (Maven) — comes built-in on recent Eclipse IDE for Java/Enterprise Java Developers

## 2. MySQL Setup
No need to manually create the schema — `spring.jpa.hibernate.ddl-auto=update`
will create tables automatically on first run. You only need the database
user to exist and have privileges. The app will auto-create the
`rideshare_db` database itself (via `createDatabaseIfNotExist=true`).

Update credentials in `src/main/resources/application.properties` if your
MySQL username/password isn't `root` / `root`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/rideshare_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root
```

## 3. Import into Eclipse
1. `File` → `Import` → `Maven` → `Existing Maven Projects`
2. Browse to the extracted `rideshare-backend` folder → `Finish`
3. Eclipse (m2e) will download dependencies automatically. Wait for the build to finish.
4. Right-click `RideshareBackendApplication.java` → `Run As` → `Java Application`
5. Server starts on **http://localhost:8080**
6. Open **http://localhost:8080** in your browser - you'll land on the login page.
   Click "Register here" to create a Rider account and a Driver account
   (use two different browsers, or one normal + one incognito window, so you
   can be logged in as both at once to test the full flow).

(You can also run it from a terminal with `mvn spring-boot:run`, or package
with `mvn clean package` and run `java -jar target/rideshare-backend.jar`.)

## 4. Project Structure
```
com.rideshare
 ├── config/            WebSocket, CORS, PasswordEncoder, and session-auth interceptor config
 ├── entity/            User, Ride, Payment, and enums (Role, RideStatus, VehicleType, PaymentStatus, PaymentMethod)
 ├── repository/        Spring Data JPA repositories
 ├── dto/                Request/response payloads
 ├── service/           UserService, MatchingService, FareCalculationService, RideService, PaymentService
 ├── controller/        REST API: AuthController, DriverController, RideController (under /api/**)
 ├── controller/web/    Thymeleaf page controllers: Auth, Rider, Driver, Payment, Home
 ├── websocket/         RealtimeBroadcaster (STOMP push helper)
 ├── exception/         Custom exceptions + global @RestControllerAdvice handler
 └── util/              DistanceUtil (Haversine formula)

resources/
 ├── templates/         Thymeleaf HTML pages (login, register, rider/*, driver/*, payment/*)
 └── static/css/        Shared stylesheet
```

## 5. Core Domain Logic

### Driver Matching (`MatchingService`)
On ride request, all `AVAILABLE` drivers of the requested `vehicleType` are
scanned, filtered to those within a 10km radius (Haversine distance), and
the nearest one is assigned. Rejected drivers are tracked per-ride so a
`reject` triggers automatic re-matching, excluding drivers who already declined.

### Fare Calculation (`FareCalculationService`)
```
fare = (baseFare + distanceKm * perKmRate + durationMin * perMinRate)
       * vehicleTypeMultiplier * surgeMultiplier
```
- Base fare: ₹40, ₹12/km, ₹1.5/min, minimum fare ₹60
- Vehicle multipliers: BIKE 1.0x, AUTO 1.2x, SEDAN 1.6x, SUV 2.0x
- Simple time-of-day surge: 1.5x during 8–10 AM and 6–9 PM

### Real-time Updates (WebSocket/STOMP)
Connect to `ws://localhost:8080/ws` (SockJS + STOMP client). Subscribe to:
- `/topic/driver-location.{driverId}` — live GPS pushes when a driver updates location
- `/topic/ride.{rideId}` — ride status transitions (assigned/accepted/started/completed/cancelled)
- `/topic/driver-rides.{driverId}` — new ride offers pushed to a specific driver

## 6. API Reference

### Auth
| Method | Endpoint | Body |
|---|---|---|
| POST | `/api/auth/register` | `{ name, email, phone, password, role: "RIDER"|"DRIVER", vehicleType?, vehicleNumber? }` |
| POST | `/api/auth/login` | `{ email, password }` |

### Drivers
| Method | Endpoint | Description |
|---|---|---|
| PUT | `/api/drivers/{driverId}/location` | Body: `{ lat, lng }` — push live GPS |
| PUT | `/api/drivers/{driverId}/availability` | Body: `{ available: true|false }` — go online/offline |
| GET | `/api/drivers/nearby?lat=&lng=&radiusKm=5` | List nearby available drivers |
| GET | `/api/drivers/{driverId}` | Get driver details |

### Rides
| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/rides/request` | Body: `{ riderId, pickupLat, pickupLng, dropLat, dropLng, vehicleType }` |
| POST | `/api/rides/{rideId}/retry-match` | Re-attempt matching if `NO_DRIVER_FOUND` |
| POST | `/api/rides/{rideId}/accept?driverId=` | Driver accepts assigned ride |
| POST | `/api/rides/{rideId}/reject?driverId=` | Driver rejects → auto re-match |
| POST | `/api/rides/{rideId}/start?driverId=` | Driver starts trip |
| POST | `/api/rides/{rideId}/complete?driverId=` | Ends trip, calculates fare |
| POST | `/api/rides/{rideId}/cancel` | Body (optional): `{ reason }` |
| GET | `/api/rides/{rideId}` | Get ride details |
| GET | `/api/rides/rider/{riderId}` | Ride history for a rider |
| GET | `/api/rides/driver/{driverId}` | Ride history for a driver |

## 7. Example Walkthrough (curl)

```bash
# 1. Register a driver
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "name":"Rahul Driver","email":"rahul@example.com","phone":"9990001111",
  "password":"pass123","role":"DRIVER","vehicleType":"SEDAN","vehicleNumber":"MH12AB1234"
}'

# 2. Register a rider
curl -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "name":"Priya Rider","email":"priya@example.com","phone":"9990002222",
  "password":"pass123","role":"RIDER"
}'

# 3. Driver goes online with a location (assume driverId=1)
curl -X PUT http://localhost:8080/api/drivers/1/location -H "Content-Type: application/json" -d '{"lat":19.0760,"lng":72.8777}'
curl -X PUT http://localhost:8080/api/drivers/1/availability -H "Content-Type: application/json" -d '{"available":true}'

# 4. Rider requests a ride (assume riderId=2)
curl -X POST http://localhost:8080/api/rides/request -H "Content-Type: application/json" -d '{
  "riderId":2,"pickupLat":19.0700,"pickupLng":72.8800,
  "dropLat":19.1000,"dropLng":72.9000,"vehicleType":"SEDAN"
}'

# 5. Driver accepts, starts, completes (assume rideId=1, driverId=1)
curl -X POST "http://localhost:8080/api/rides/1/accept?driverId=1"
curl -X POST "http://localhost:8080/api/rides/1/start?driverId=1"
curl -X POST "http://localhost:8080/api/rides/1/complete?driverId=1"
```

## 8. Using the Web App (end-to-end walkthrough)

1. **Register a driver**: go to `/register`, choose role "Driver", fill in
   vehicle type/number. Log in.
2. On the driver dashboard, click **Go Online**. Your browser will ask for
   location permission — allow it. Your live location starts being pushed to
   the server automatically every ~5 seconds and shown on your map.
3. **Register a rider** in a second browser/incognito window, choose role
   "Rider". Log in.
4. On the rider dashboard, click the map to set a **pickup point** (near
   where your driver is, so matching succeeds within the 10km radius), then
   click again to set a **destination**. Pick a vehicle type that matches
   your driver, then click **Request Ride**.
5. The app automatically matches the nearest available driver. The driver's
   dashboard immediately redirects to the ride offer screen (via WebSocket) —
   click **Accept**.
6. The rider's ride page now shows the driver's live location moving on the
   map in real time.
7. Driver clicks **Start Trip**, then **Complete Trip** — fare is calculated
   automatically from the trip distance/duration.
8. Rider sees the fare and clicks **Pay Now** → picks a payment method →
   gets an instant (simulated) receipt.

Note: the map/geocoding/WebSocket libraries load from public CDNs (unpkg,
jsdelivr) and OpenStreetMap, so the machine running the browser needs
internet access for the map tiles and address lookups to work — the backend
and database can still be entirely local.

## 9. Possible Extensions (good talking points for system design interviews)
- Replace in-memory Haversine scan with MySQL spatial indexes or Redis GEO for scale
- Add JWT-based authentication/authorization (Spring Security)
- Add a scheduled job to auto-retry `NO_DRIVER_FOUND` rides
- Add surge pricing based on real-time supply/demand ratio instead of fixed time windows
- Add rider/driver rating submission after ride completion
- Move to event-driven architecture (Kafka) for location updates at scale
- Add read replicas / caching layer for driver availability lookups
