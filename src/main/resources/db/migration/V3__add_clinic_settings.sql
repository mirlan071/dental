CREATE TABLE clinic_settings (
    id BIGINT PRIMARY KEY,
    workday_start TIME NOT NULL,
    workday_end TIME NOT NULL,
    timezone VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_clinic_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_clinic_settings_hours CHECK (workday_start < workday_end)
);

INSERT INTO clinic_settings (id, workday_start, workday_end, timezone)
VALUES (1, TIME '09:00', TIME '18:00', 'Asia/Bishkek');
