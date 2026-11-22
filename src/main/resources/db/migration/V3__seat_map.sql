-- Seat map for events with assigned seating.
-- Each row is one physical seat; status tracks whether it is free, temporarily held during checkout, or sold.
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    ticket_type_id BIGINT NOT NULL REFERENCES ticket_types(id),
    section VARCHAR(50) NOT NULL,
    row_label VARCHAR(50) NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    held_by BIGINT REFERENCES users(id),
    held_until TIMESTAMP,
    order_id BIGINT REFERENCES orders(id),
    UNIQUE (event_id, section, row_label, seat_number)
);

CREATE INDEX IF NOT EXISTS idx_seats_event ON seats(event_id);
CREATE INDEX IF NOT EXISTS idx_seats_order ON seats(order_id);
