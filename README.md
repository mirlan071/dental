# Dental CRM backend

Java 25 / Spring Boot modular monolith for a small dental clinic.

## Run locally

Prerequisites: Docker Desktop, Java 25, Maven, and Node.js/npm.

Start PostgreSQL from the project root:

```powershell
docker compose up -d
```

Start the backend with Java 25 and the development profile:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

In a second terminal, start the frontend:

```powershell
cd frontend
npm run dev
```

Local services are available at `localhost:5173` (frontend), `localhost:8080` (backend), and `localhost:5432` (PostgreSQL). Stop PostgreSQL with `docker compose down`; its data remains in the named Docker volume.

Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to override the local datasource defaults when needed.

The `dev` profile inserts `admin/admin123` plus the realistic demo dataset and demo doctor accounts documented below. These credentials are only for demo/development use.

Authentication is session-based. `POST /api/auth/login` with JSON credentials, then reuse the returned `JSESSIONID` cookie.
Before state-changing requests, call `GET /api/auth/csrf` and send its token in the returned header name.

## Production deployment

Target architecture:

```text
Vercel
  React / Vite frontend
        |
      HTTPS
        v
Spring Boot backend container
        |
        v
Neon PostgreSQL
```

### Backend

Run the backend with `SPRING_PROFILES_ACTIVE=prod`. Required environment variables:

- `SPRING_DATASOURCE_URL` — Neon JDBC URL containing `sslmode=require`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_FRONTEND_URL` — exact HTTPS frontend origin, without a trailing slash
- `PORT` — optional, defaults to `8080`

Use the pooled Neon hostname for the application datasource. `SPRING_FLYWAY_URL` is optional and may point to the direct Neon endpoint for migrations; it defaults to `SPRING_DATASOURCE_URL`. Flyway runs automatically during startup and Hibernate validates the resulting schema.

For the first production administrator, optionally set all three variables before the initial startup:

- `APP_BOOTSTRAP_ADMIN_USERNAME`
- `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_FULL_NAME`

The account is created only when no ADMIN exists. Remove the bootstrap password from the hosting environment after successful initialization. Development demo data is restricted to the `dev` profile.

To load the realistic presentation dataset once in production, start the application with both:

- `SPRING_PROFILES_ACTIVE=prod`
- `APP_LOAD_DEMO_DATA=true`

The seed requires an existing ADMIN, never changes that account, and records completion in `app_data_seeds` so later restarts do not duplicate records. Set `APP_LOAD_DEMO_DATA=false` again after the successful startup. A fresh database with an ADMIN receives 5 active demo doctors, 1 inactive demo doctor, 11 services, 50 patients, about 108 appointments, and roughly 56–70 payments (the exact payment count depends on the time of day when today's appointments are seeded).

The active demo doctor usernames are `demo.aizada`, `demo.azamat`, `demo.ruslan`, `demo.aibek`, and `demo.nuriza`. The inactive account is `demo.bakyt`. These clearly demo-only accounts use the shared password `DemoClinic-2026!`; passwords are BCrypt-hashed and are never written to application logs.

Build the backend container from the repository root:

```powershell
docker build -t dental-crm-backend .
```

The unauthenticated hosting health check is `GET /api/health`.

### Vercel frontend

Configure the Vercel project with:

- Root Directory: `frontend`
- Build Command: `npm run build`
- Output Directory: `dist`

`frontend/vercel.json` rewrites same-origin `/api/*` requests to the Render backend before applying the React Router SPA fallback. No `VITE_API_URL` environment variable is required.

The browser sends session and CSRF traffic to the Vercel frontend origin; Vercel proxies those requests to the backend over HTTPS.
