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

The `dev` profile inserts sample users (`admin/admin123`, `doctor/doctor123`). Change these credentials outside local development.

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
- Environment variable: `VITE_API_URL=https://your-backend-origin.example`

`frontend/vercel.json` provides the React Router SPA fallback. API traffic is sent directly to `VITE_API_URL`; it is not rewritten through Vercel.

Cross-origin session authentication requires HTTPS, credentialed CORS, and secure `SameSite=None` cookies. Prefer sibling custom domains under the same parent domain for the frontend and backend to avoid browser third-party-cookie restrictions.
