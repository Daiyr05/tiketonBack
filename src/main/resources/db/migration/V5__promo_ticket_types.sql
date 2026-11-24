-- Ticket types a promotional campaign applies to (SRS 4.14). No rows = the campaign applies to every ticket type.
CREATE TABLE IF NOT EXISTS promotional_campaign_ticket_types (
    campaign_id BIGINT NOT NULL REFERENCES promotional_campaigns(id) ON DELETE CASCADE,
    ticket_type_id BIGINT NOT NULL REFERENCES ticket_types(id) ON DELETE CASCADE,
    PRIMARY KEY (campaign_id, ticket_type_id)
);
