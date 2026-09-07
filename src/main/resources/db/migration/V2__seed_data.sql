-- Seed data: runs once on first migration, never again

-- All demo accounts password: 123
INSERT INTO users (email, password, first_name, last_name, role, email_verified, suspended)
VALUES ('admin@biletflow.kz', '$2b$10$VerBOK5dBbl/AWDLLpi2GO9rqlWPUlw7EdA9t1S4V3HuPofFJn/8u', 'Platform', 'Admin', 'PLATFORM_ADMIN', true, false);

-- Demo Organizer
INSERT INTO users (email, password, first_name, last_name, role, email_verified, suspended)
VALUES ('organizer@biletflow.kz', '$2b$10$VerBOK5dBbl/AWDLLpi2GO9rqlWPUlw7EdA9t1S4V3HuPofFJn/8u', 'Demo', 'Organizer', 'ORGANIZER', true, false);

-- Demo Attendee
INSERT INTO users (email, password, first_name, last_name, role, email_verified, suspended)
VALUES ('attendee@biletflow.kz', '$2b$10$VerBOK5dBbl/AWDLLpi2GO9rqlWPUlw7EdA9t1S4V3HuPofFJn/8u', 'Demo', 'Attendee', 'ATTENDEE', true, false);

-- Demo Event Admin
INSERT INTO users (email, password, first_name, last_name, role, email_verified, suspended)
VALUES ('eventadmin@biletflow.kz', '$2b$10$VerBOK5dBbl/AWDLLpi2GO9rqlWPUlw7EdA9t1S4V3HuPofFJn/8u', 'Demo', 'EventAdmin', 'EVENT_ADMIN', true, false);

-- Organizer Profile for demo organizer
INSERT INTO organizer_profiles (user_id, organization_name, description, contact_email, contact_phone, identity_verified, payout_verified)
VALUES (2, 'BiletFlow Demo Org', 'Demo organizer for testing', 'organizer@biletflow.kz', '+7 777 123 4567', true, true);

-- Demo Event (published, free)
INSERT INTO events (organizer_id, title, description, category, venue_name, venue_address, city, event_date, event_end_date, registration_open_date, registration_close_date, capacity, status, visibility, paid_sales_activated, assigned_seating)
VALUES (2, 'BiletFlow Demo Concert', 'A demo free concert event for testing the platform.', 'Music', 'Almaty Arena', 'Rozybakiev St 247', 'Almaty', NOW() + INTERVAL '30 days', NOW() + INTERVAL '30 days' + INTERVAL '3 hours', NOW(), NOW() + INTERVAL '29 days', 500, 'PUBLISHED', 'PUBLIC', false, false);

-- Demo Event (published, paid, activated)
INSERT INTO events (organizer_id, title, description, category, venue_name, venue_address, city, event_date, event_end_date, registration_open_date, registration_close_date, capacity, status, visibility, paid_sales_activated, assigned_seating)
VALUES (2, 'Tech Conference Astana 2026', 'Annual tech conference with top speakers from Central Asia.', 'Technology', 'EXPO Convention Center', 'Mangilik El Ave 55/1', 'Astana', NOW() + INTERVAL '60 days', NOW() + INTERVAL '60 days' + INTERVAL '8 hours', NOW(), NOW() + INTERVAL '59 days', 1000, 'PUBLISHED', 'PUBLIC', true, false);

-- Free ticket type for demo concert
INSERT INTO ticket_types (event_id, name, description, price, quantity, sold, free, hidden)
VALUES (1, 'General Admission', 'Free entry to the demo concert', 0.00, 500, 0, true, false);

-- Paid ticket types for tech conference
INSERT INTO ticket_types (event_id, name, description, price, quantity, sold, free, hidden, max_per_order)
VALUES (2, 'Early Bird', 'Discounted early bird ticket', 5000.00, 200, 0, false, false, 5);

INSERT INTO ticket_types (event_id, name, description, price, quantity, sold, free, hidden, max_per_order)
VALUES (2, 'Standard', 'Standard conference ticket', 10000.00, 500, 0, false, false, 10);

INSERT INTO ticket_types (event_id, name, description, price, quantity, sold, free, hidden, max_per_order)
VALUES (2, 'VIP', 'VIP access with front row seating and meet & greet', 25000.00, 50, 0, false, false, 2);

-- Staff assignment: event admin assigned to both events
INSERT INTO staff_assignments (event_id, user_id) VALUES (1, 4);
INSERT INTO staff_assignments (event_id, user_id) VALUES (2, 4);

-- Promo campaign for tech conference
INSERT INTO promotional_campaigns (event_id, name, discount_type, discount_value, max_redemptions, current_redemptions, valid_from, valid_to, active)
VALUES (2, 'Launch Discount', 'PERCENTAGE', 20.00, 100, 0, NOW(), NOW() + INTERVAL '30 days', true);

-- Promo code for the campaign
INSERT INTO promo_codes (campaign_id, code, redemptions)
VALUES (1, 'BF-LAUNCH20', 0);

-- Audit log entries
INSERT INTO audit_logs (event_id, user_id, action_type, entity_type, entity_id, description)
VALUES (1, 2, 'CREATE', 'Event', 1, 'Event created: BiletFlow Demo Concert');

INSERT INTO audit_logs (event_id, user_id, action_type, entity_type, entity_id, description)
VALUES (1, 2, 'PUBLISH', 'Event', 1, 'Event published');

INSERT INTO audit_logs (event_id, user_id, action_type, entity_type, entity_id, description)
VALUES (2, 2, 'CREATE', 'Event', 2, 'Event created: Tech Conference Astana 2026');

INSERT INTO audit_logs (event_id, user_id, action_type, entity_type, entity_id, description)
VALUES (2, 2, 'PUBLISH', 'Event', 2, 'Event published');

INSERT INTO audit_logs (event_id, user_id, action_type, entity_type, entity_id, description)
VALUES (2, 2, 'ACTIVATE_PAID_SALES', 'Event', 2, 'Paid sales activated (simulated)');
