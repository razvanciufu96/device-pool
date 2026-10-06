CREATE TABLE app_user (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL UNIQUE,
    role  VARCHAR(20)  NOT NULL
);

CREATE TABLE device (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    type      VARCHAR(20)  NOT NULL,
    os        VARCHAR(100) NOT NULL,
    asset_tag VARCHAR(50)  NOT NULL UNIQUE
);

-- Intervals are half-open: [start_at, end_at). A booking ending at 14:00 and one
-- starting at 14:00 do not overlap.
-- Cancelled reservations are kept (cancelled_at set) so history is not lost.
CREATE TABLE reservation (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id    BIGINT NOT NULL REFERENCES device (id),
    user_id      BIGINT NOT NULL REFERENCES app_user (id),
    start_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    end_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    cancelled_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT reservation_valid_range CHECK (end_at > start_at)
);

CREATE INDEX reservation_device_time ON reservation (device_id, start_at, end_at);
CREATE INDEX reservation_user ON reservation (user_id);
