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
