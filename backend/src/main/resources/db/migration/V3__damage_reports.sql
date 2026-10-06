-- Wish C: damage tracking.
-- device.damaged is kept in sync with open reports by DamageService. It's a deliberate
-- denormalization so the booking code can check it on the row it already locks.
ALTER TABLE device ADD COLUMN damaged BOOLEAN DEFAULT FALSE NOT NULL;

CREATE TABLE damage_report (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id   BIGINT        NOT NULL REFERENCES device (id),
    reported_by BIGINT        NOT NULL REFERENCES app_user (id),
    description VARCHAR(1000) NOT NULL,
    reported_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    resolved_by BIGINT REFERENCES app_user (id),
    resolved_at TIMESTAMP(6) WITH TIME ZONE
);

CREATE INDEX damage_report_device ON damage_report (device_id);
