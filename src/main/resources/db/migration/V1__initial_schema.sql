CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE users (
 id BIGSERIAL PRIMARY KEY, username VARCHAR(100) NOT NULL, password_hash VARCHAR(255) NOT NULL,
 full_name VARCHAR(200) NOT NULL, role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(), CONSTRAINT uk_users_username UNIQUE (username),
 CONSTRAINT ck_users_role CHECK (role IN ('ADMIN','DOCTOR'))
);
CREATE UNIQUE INDEX uk_users_username_lower ON users (lower(username));

CREATE TABLE doctors (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
 specialization VARCHAR(200), phone VARCHAR(30)
);
CREATE TABLE patients (
 id BIGSERIAL PRIMARY KEY, full_name VARCHAR(200) NOT NULL, phone VARCHAR(30) NOT NULL,
 birth_date DATE, notes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_patients_phone ON patients(phone);
CREATE INDEX idx_patients_full_name ON patients(full_name);

CREATE TABLE services (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(200) NOT NULL UNIQUE, price NUMERIC(12,2) NOT NULL,
 duration_minutes INTEGER NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT ck_services_price CHECK(price >= 0), CONSTRAINT ck_services_duration CHECK(duration_minutes > 0)
);

CREATE TABLE appointments (
 id BIGSERIAL PRIMARY KEY, patient_id BIGINT NOT NULL REFERENCES patients(id), doctor_id BIGINT NOT NULL REFERENCES doctors(id),
 start_time TIMESTAMPTZ NOT NULL, end_time TIMESTAMPTZ NOT NULL, status VARCHAR(30) NOT NULL,
 notes TEXT, created_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by BIGINT NOT NULL REFERENCES users(id),
 CONSTRAINT ck_appointment_time CHECK(end_time > start_time),
 CONSTRAINT ck_appointment_status CHECK(status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED','NO_SHOW'))
);
CREATE INDEX idx_appointments_doctor_start ON appointments(doctor_id,start_time);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_status_start ON appointments(status,start_time);
ALTER TABLE appointments ADD CONSTRAINT ex_appointments_doctor_overlap EXCLUDE USING gist
 (doctor_id WITH =, tstzrange(start_time,end_time,'[)') WITH &&)
 WHERE (status NOT IN ('CANCELLED','NO_SHOW'));

CREATE TABLE appointment_services (
 id BIGSERIAL PRIMARY KEY, appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
 service_id BIGINT NOT NULL REFERENCES services(id), price NUMERIC(12,2) NOT NULL,
 CONSTRAINT uk_appointment_service UNIQUE(appointment_id,service_id), CONSTRAINT ck_appointment_service_price CHECK(price >= 0)
);
CREATE INDEX idx_appointment_services_appointment ON appointment_services(appointment_id);

CREATE TABLE payments (
 id BIGSERIAL PRIMARY KEY, appointment_id BIGINT NOT NULL REFERENCES appointments(id), amount NUMERIC(12,2) NOT NULL,
 payment_method VARCHAR(20) NOT NULL, paid_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT ck_payment_amount CHECK(amount > 0), CONSTRAINT ck_payment_method CHECK(payment_method IN ('CASH','CARD','QR'))
);
CREATE INDEX idx_payments_appointment ON payments(appointment_id);
CREATE INDEX idx_payments_paid_at ON payments(paid_at);
CREATE INDEX idx_payments_method_paid_at ON payments(payment_method,paid_at);
