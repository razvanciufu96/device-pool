# Device Pool

A small web app that replaces the shared spreadsheet for the pool of physical test devices.
People can see what's free now and tomorrow, reserve a device for a time range (never double-booked),
cancel their reservations, and report damaged devices.

- **Backend:** Java 25, Spring Boot 4, Spring Data JPA, Flyway, H2 (file-based)
- **Frontend:** Vue 3, Vue Router, Vite

## How to run

### Option 1: JDK 25 + Node.js (no Docker)

Requirements: **JDK 25** (`JAVA_HOME` pointing to it) and **Node.js 20.19+ / 22.12+**.
Maven is not needed; the Maven Wrapper downloads it.

Terminal 1, backend (http://localhost:8080):

```bash
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Terminal 2, frontend (http://localhost:5173):

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173**. The Vite dev server forwards `/api` to the backend.

The database is stored in `backend/data/`. To start over with fresh demo data, stop the backend and delete that folder.

Run the backend tests with `cd backend && ./mvnw test`.

### Option 2: Docker Compose

```bash
docker compose up --build
```

- App: **http://localhost:3000** (nginx serves the built frontend and proxies `/api` to the backend)
- Backend API directly: http://localhost:8080/api/devices

Data is kept in the `h2-data` volume. `docker compose down -v` resets it.
Demo reservations are created in the `Europe/Bucharest` time zone; override it with `TZ=Europe/Berlin docker compose up --build`.

## How to use

### Logging in

There are no passwords. The login page lists the users, and you pick one. Your choice is remembered in the browser,
and **Switch user** in the top bar takes you back to the list.

| User          | Role       | Notes                                                    |
|---------------|------------|----------------------------------------------------------|
| Ana Popescu   | member     | Currently has the Pixel 8; had the iPhone SE yesterday   |
| Mihai Ionescu | member     | Has the ThinkPad soon and the iPhone 15 tomorrow afternoon |
| Elena Dumitru | lead       | Has the Galaxy Tab tomorrow morning                      |
| Radu Stan     | facility   | The only one who can mark damaged devices as repaired    |

### Demo data

9 devices (iPhones, Android phones, tablets, two old laptops with legacy browsers). On the first start, a few
reservations are created relative to the current time, and the iPhone SE is reported as damaged.

### Walkthrough

1. **Log in** as *Mihai Ionescu*.
2. **Devices → Free now:** the Pixel 8 is *In use* by Ana and the iPhone SE is *Damaged*. Filter by phones, tablets or laptops.
3. **Devices → Tomorrow:** the iPhone 15 and Galaxy Tab are *Partly booked*, with their time slots shown.
4. **Reserve:** click *Reserve* on the Pixel 8 and submit. The reservation is rejected, and the message says who has it and until when.
   Reserve a free device instead; the quick buttons fill in *Next hour* or *Tomorrow morning/afternoon*.
5. **My reservations:** shows current/upcoming reservations and history. *Cancel* an upcoming one; on a running one the button is *Return now*.
6. **Report damage** on any device: it's taken out of the pool immediately.
7. **Damage reports:** shows what is broken, who reported it, and the people who **last held** the device.
   Switch to *Radu Stan* (facility) to *Mark repaired*.

## API overview

All endpoints are under `/api`. The current user is sent in the `X-User-Id` header (the fake login).
Errors are returned as RFC 9457 problem details.

| Method | Path                                   | Description                                                      |
|--------|----------------------------------------|------------------------------------------------------------------|
| GET    | `/users`                               | Users for the login picker                                       |
| GET    | `/devices?from=&to=`                   | Devices with availability for a time window (default: now)       |
| POST   | `/reservations`                        | `{deviceId, start, end}` → 201, or 409 with the conflicting bookings |
| GET    | `/reservations/mine`                   | The current user's reservations                                  |
| DELETE | `/reservations/{id}`                   | Cancel (upcoming) or end now (running), own reservations only     |
| POST   | `/devices/{id}/damage-reports`         | `{description}`: report damage                                   |
| GET    | `/damage-reports`                      | All reports, including the last holders                          |
| POST   | `/damage-reports/{id}/resolve`         | Mark repaired (facility role only)                               |

## Project structure

```
backend/
  src/main/java/com/devicepool/
    reservation/   booking, cancelling, the overlap guarantee
    damage/        damage reports (wish C)
    device/        device list with availability
    user/          users and the fake current-user lookup
    common/        error handling, clock
    demo/          demo reservations relative to "now"
  src/main/resources/db/migration/   Flyway schema + seed data
frontend/
  src/views/       Login, Devices, My reservations, Damage reports
  src/components/  Booking and damage dialogs
  src/api.js       the only place that talks to the backend
docker-compose.yml
```

See [NOTES.md](NOTES.md) for decisions, trade-offs and open questions.
