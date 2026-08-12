ALTER TABLE appointment_services
    ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1;

ALTER TABLE appointment_services
    ADD CONSTRAINT ck_appointment_service_quantity CHECK (quantity >= 1);
