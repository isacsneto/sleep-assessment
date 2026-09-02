-- A single night's sleep log for a user.
--
-- in_bed_start / in_bed_end are stored as local wall-clock timestamps (the time
-- the user got to bed and got out of bed). Storing full timestamps rather than
-- bare times keeps the "time in bed" interval correct when sleep crosses
-- midnight (e.g. 10:53 pm -> 7:05 am).
--
-- total_minutes_in_bed is derived from the interval at write time and persisted
-- so reads and average calculations do not have to recompute it.
CREATE TABLE sleep_logs (
    id                   BIGSERIAL   PRIMARY KEY,
    user_id              BIGINT      NOT NULL REFERENCES users (id),
    sleep_date           DATE        NOT NULL,
    in_bed_start         TIMESTAMP   NOT NULL,
    in_bed_end           TIMESTAMP   NOT NULL,
    total_minutes_in_bed INTEGER     NOT NULL,
    morning_feeling      VARCHAR(4)  NOT NULL,
    created_at           TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT chk_sleep_logs_feeling CHECK (morning_feeling IN ('BAD', 'OK', 'GOOD')),
    CONSTRAINT chk_sleep_logs_interval CHECK (in_bed_end > in_bed_start),
    CONSTRAINT chk_sleep_logs_total_minutes CHECK (total_minutes_in_bed > 0),
    -- At most one sleep log per user per night.
    CONSTRAINT uq_sleep_logs_user_date UNIQUE (user_id, sleep_date)
);

-- The two dominant access patterns fetch a user's most recent log and a user's
-- logs within a date range; both are served by this index.
CREATE INDEX idx_sleep_logs_user_date ON sleep_logs (user_id, sleep_date DESC);
