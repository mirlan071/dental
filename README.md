# Dental CRM backend

Java 25 / Spring Boot modular monolith for a small dental clinic.

## Run locally

1. Create PostgreSQL database `dental_crm`.
2. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` if they differ from the defaults in `application.yml`.
3. Run `mvn spring-boot:run -Dspring-boot.run.profiles=dev`.

The `dev` profile inserts sample users (`admin/admin123`, `doctor/doctor123`). Change these credentials outside local development.

Authentication is session-based. `POST /api/auth/login` with JSON credentials, then reuse the returned `JSESSIONID` cookie.
Before state-changing requests, call `GET /api/auth/csrf` and send its token in the returned header name.
