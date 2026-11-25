-- Notifications: optional link to the related page in the frontend
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS link VARCHAR(500);

-- Seats reserved for people with disabilities (SRS 4.3.1)
ALTER TABLE seats ADD COLUMN IF NOT EXISTS accessible BOOLEAN NOT NULL DEFAULT FALSE;

-- Service fee charged by the platform on each order (SRS 4.6)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS service_fee NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- Refund policy shown to attendees (SRS 4.9)
ALTER TABLE events ADD COLUMN IF NOT EXISTS refund_policy TEXT;

-- Platform-wide settings (single row, id = 1)
CREATE TABLE IF NOT EXISTS platform_settings (
    id BIGINT PRIMARY KEY,
    service_fee_percent NUMERIC(5, 2) NOT NULL DEFAULT 0,
    service_fee_fixed NUMERIC(12, 2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP
);
INSERT INTO platform_settings (id, service_fee_percent, service_fee_fixed, updated_at)
VALUES (1, 0, 0, NOW())
ON CONFLICT (id) DO NOTHING;

-- Reports about events filed by users (SRS 4.12)
CREATE TABLE IF NOT EXISTS event_reports (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    admin_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_event_reports_status ON event_reports(status);
