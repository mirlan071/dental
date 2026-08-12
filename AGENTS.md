# Dental CRM

## Product

Dental CRM is a lightweight CRM for a small dental clinic in Kyrgyzstan.

The product should remain simple, practical and fast for clinic administrators and doctors.

Primary roles:

- ADMIN
- DOCTOR

ADMIN manages the clinic, doctors, patients, services, appointments, payments, debts, analytics and settings.

DOCTOR works mainly with their own appointments, patients and personal dashboard.

UI language is Russian.

Currency is Kyrgyz som: `сом`.

Clinic timezone: `Asia/Bishkek`.

## Technology

Backend:

- Java 25
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Maven

Frontend:

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Tailwind CSS
- Radix / shadcn-style components

Architecture: modular monolith.

Do not introduce microservices unless explicitly requested.

## Authentication

Authentication is session based.

Do not introduce JWT unless explicitly requested.

Keep Spring Security enabled. Keep CSRF enabled. Do not weaken authorization just to make frontend implementation easier.

Passwords use BCrypt.

## Database

PostgreSQL is the source of truth.

Use Flyway for schema changes. Never modify previously applied Flyway migrations. Create a new migration for schema changes.

Use `spring.jpa.hibernate.ddl-auto=validate`. Do not use create or update schema generation.

Preserve historical appointment, service, payment and debt data.

## Money

Use BigDecimal for monetary calculations.

Payment methods: CASH, CARD, QR.

QR is a separate payment category because this product is designed for Kyrgyzstan.

Do not calculate doctor salary or commission unless explicitly requested.

## Appointment model

Appointments have Patient, Doctor, scheduled startTime, scheduled endTime, status, services and payments.

Statuses: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW.

Preserve existing transition rules. Do not bypass appointment conflict detection.

Cancelled and NO_SHOW appointments do not block schedule availability according to the existing backend behavior.

## Services

Appointments may contain multiple services.

AppointmentServiceItem stores a historical snapshot price. Changing ClinicService.price must never modify historical appointment totals.

Service quantity is supported.

Do not expose JPA entities directly from controllers. Use DTOs.

## Payments and debt

Payments belong to appointments. Partial and split payments are supported.

Payment methods: CASH, CARD, QR.

Debt is derived from completed appointment totals minus all payments associated with those appointments.

Do not introduce a separate Credit entity unless there is a genuine requirement that the existing model cannot represent.

## UI

The product should feel like a modern professional medical SaaS application.

Prefer clean white surfaces, light neutral backgrounds, subtle borders, teal / emerald accent, high readability, comfortable touch targets and simple workflows.

Avoid browser-native ugly controls in important workflows, heavy gradients, glassmorphism, neon effects, excessive animation and unnecessary decorative UI.

Desktop and mobile may use different layouts when appropriate. Do not simply shrink desktop layouts onto mobile screens.

## Calendar

Desktop uses a daily schedule grid with time vertically and doctors horizontally.

Mobile uses an agenda / timeline optimized for touch.

Do not regress either implementation.

ClinicDatePicker is custom. Do not restore browser-native date input.

## Development principles

Inspect existing implementation before adding code.

Reuse existing components, services and APIs where practical. Prefer minimal changes. Do not create unnecessary abstractions. Do not create interfaces for every service without a reason.

Do not redesign working architecture unless explicitly requested. Preserve existing functionality when implementing new features.

## Explicitly deferred features

Do not add these unless explicitly requested:

- overtime appointments
- actual treatment start/end tracking
- individual doctor schedules
- doctor shifts
- salary
- commissions
- payroll
- multi-clinic
- inventory
- tooth chart
- SMS
- WhatsApp
- WebSockets
- Redis
- RabbitMQ
- Kafka
- microservices
- JWT
- week calendar
- month calendar
- drag and drop

## Testing

After backend changes, run the complete backend test suite.

After frontend changes, run `npm run build`.

Fix compilation, TypeScript and test failures before considering the task complete. Run `git diff --check` where useful.

## Git

Do not commit automatically. Do not push automatically.

Do not reset or delete realistic development data unless explicitly requested.

Do not modify unrelated dirty files.
