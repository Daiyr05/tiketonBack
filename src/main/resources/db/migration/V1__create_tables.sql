-- Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(255),
    reset_password_token VARCHAR(255),
    reset_password_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    suspended BOOLEAN DEFAULT FALSE
);

-- Organizer Profiles
CREATE TABLE organizer_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    organization_name VARCHAR(255),
    description TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    website VARCHAR(255),
    payout_bank_name VARCHAR(255),
    payout_account_number VARCHAR(255),
    payout_account_holder VARCHAR(255),
    identity_verified BOOLEAN DEFAULT FALSE,
    payout_verified BOOLEAN DEFAULT FALSE
);

-- Events
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    image_url VARCHAR(500),
    venue_name VARCHAR(255),
    venue_address VARCHAR(500),
    city VARCHAR(255),
    event_date TIMESTAMP,
    event_end_date TIMESTAMP,
    registration_open_date TIMESTAMP,
    registration_close_date TIMESTAMP,
    capacity INTEGER,
    status VARCHAR(50) NOT NULL,
    visibility VARCHAR(50) NOT NULL,
    paid_sales_activated BOOLEAN DEFAULT FALSE,
    assigned_seating BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Ticket Types
CREATE TABLE ticket_types (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    sold INTEGER NOT NULL DEFAULT 0,
    free BOOLEAN DEFAULT FALSE,
    hidden BOOLEAN DEFAULT FALSE,
    sales_start TIMESTAMP,
    sales_end TIMESTAMP,
    max_per_order INTEGER
);

-- Promotional Campaigns
CREATE TABLE promotional_campaigns (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    max_redemptions INTEGER,
    current_redemptions INTEGER DEFAULT 0,
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Promo Codes
CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL REFERENCES promotional_campaigns(id),
    code VARCHAR(255) NOT NULL UNIQUE,
    redemptions INTEGER DEFAULT 0
);

-- Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    attendee_id BIGINT NOT NULL REFERENCES users(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    total_amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL,
    promo_code_id BIGINT REFERENCES promo_codes(id),
    attendee_first_name VARCHAR(255),
    attendee_last_name VARCHAR(255),
    attendee_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Order Items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    ticket_type_id BIGINT NOT NULL REFERENCES ticket_types(id),
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    discount NUMERIC(12, 2),
    seat_section VARCHAR(50),
    seat_row VARCHAR(50),
    seat_number VARCHAR(50)
);

-- Tickets
CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(255) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    ticket_type_id BIGINT NOT NULL REFERENCES ticket_types(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    attendee_id BIGINT NOT NULL REFERENCES users(id),
    attendee_name VARCHAR(255),
    attendee_email VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    seat_section VARCHAR(50),
    seat_row VARCHAR(50),
    seat_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    amount NUMERIC(12, 2) NOT NULL,
    method VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    transaction_ref VARCHAR(255),
    simulated BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Refunds
CREATE TABLE refunds (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    amount NUMERIC(12, 2) NOT NULL,
    reason TEXT,
    initiated_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Check-in Records
CREATE TABLE check_in_records (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id),
    event_id BIGINT NOT NULL REFERENCES events(id),
    checked_by BIGINT REFERENCES users(id),
    reversed BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Staff Assignments
CREATE TABLE staff_assignments (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    UNIQUE(event_id, user_id)
);

-- Support Cases
CREATE TABLE support_cases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_id BIGINT REFERENCES events(id),
    order_id BIGINT REFERENCES orders(id),
    ticket_id BIGINT REFERENCES tickets(id),
    category VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    assigned_to BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Support Messages
CREATE TABLE support_messages (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES support_cases(id),
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT,
    user_id BIGINT,
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    description TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
