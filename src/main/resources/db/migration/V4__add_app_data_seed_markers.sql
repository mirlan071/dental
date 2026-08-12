CREATE TABLE app_data_seeds (
    seed_key VARCHAR(100) PRIMARY KEY,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
