# BiletFlow API Guide (for Frontend & Mobile Developers)

## Base URL

```
http://localhost:8080
```

## Quick Start

### Option 1: Docker (recommended)
```bash
docker compose up --build
```
That's it. API is at `http://localhost:8080`, DB runs automatically.

To stop:
```bash
docker compose down
```

To wipe the database and start fresh:
```bash
docker compose down -v
docker compose up --build
```

### Option 2: Run locally
```bash
# Make sure PostgreSQL is running
psql -U postgres -c "CREATE DATABASE biletflow;"

# Run (requires Java 25)
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home ./mvnw spring-boot:run
```

---

## Swagger UI

Open **http://localhost:8080/swagger-ui.html** to see and test all endpoints interactively.

---

## Authentication

The API uses JWT Bearer tokens. Every request (except public ones) needs the header:
```
Authorization: Bearer <token>
```

### Demo Accounts (pre-seeded)

| Email | Password | Role |
|-------|----------|------|
| admin@biletflow.kz | 123 | PLATFORM_ADMIN |
| organizer@biletflow.kz | 123 | ORGANIZER |
| attendee@biletflow.kz | 123 | ATTENDEE |
| eventadmin@biletflow.kz | 123 | EVENT_ADMIN |

### Login

```
POST /api/auth/login
Content-Type: application/json

{
  "email": "organizer@biletflow.kz",
  "password": "123"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1...",
  "userId": 2,
  "email": "organizer@biletflow.kz",
  "role": "ORGANIZER",
  "firstName": "Demo",
  "lastName": "Organizer"
}
```

### Register New User

```
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "mypassword",
  "firstName": "John",
  "lastName": "Doe",
  "role": "ATTENDEE"
}
```

Role can be `ATTENDEE` or `ORGANIZER`.

---

## Public Endpoints (no token needed)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register |
| POST | /api/auth/login | Login |
| GET | /api/auth/verify?token=xxx | Verify email |
| GET | /api/events/public | List public published events |
| GET | /api/events/{id} | Get event details |
| GET | /api/events/{id}/ticket-types | Get visible ticket types |
| GET | /api/events/{id}/calendar | Download .ics calendar file |
| GET | /api/promo/validate/{code} | Validate a promo code |
| GET | /swagger-ui.html | Swagger UI |

---

## Attendee Endpoints (role: ATTENDEE)

All require `Authorization: Bearer <token>`.

### Browse & Register

```
# Get public events
GET /api/events/public

# Get event details
GET /api/events/{eventId}

# Get ticket types for an event
GET /api/events/{eventId}/ticket-types
```

### Create Order (buy/register for tickets)

```
POST /api/orders
Content-Type: application/json

{
  "eventId": 1,
  "attendeeFirstName": "John",
  "attendeeLastName": "Doe",
  "attendeeEmail": "john@example.com",
  "promoCode": "BF-LAUNCH20",
  "items": [
    {
      "ticketTypeId": 2,
      "quantity": 2
    }
  ]
}
```

- Free orders auto-complete and issue tickets immediately
- Paid orders go to PENDING status, then call `/pay`

### Complete Payment (simulated)

```
POST /api/orders/{orderId}/pay
```

### View My Orders & Tickets

```
GET /api/orders/my
GET /api/tickets/my
GET /api/tickets/order/{orderId}
```

### Download Ticket

```
# QR code image (PNG)
GET /api/tickets/{ticketId}/qr

# Printable PDF ticket
GET /api/tickets/{ticketId}/pdf
```

### Cancel Order

```
POST /api/orders/{orderId}/cancel
```

### Support

```
# Open a support case
POST /api/support/cases
Content-Type: application/json

{
  "category": "ticket_delivery",
  "subject": "Didn't receive my ticket",
  "message": "I paid but no ticket arrived",
  "eventId": 1,
  "orderId": 1
}

# Send a message
POST /api/support/cases/{caseId}/messages
{ "message": "Any update?" }

# View my cases
GET /api/support/cases/my

# View messages in a case
GET /api/support/cases/{caseId}/messages
```

### Notifications

```
GET /api/notifications
GET /api/notifications/unread
POST /api/notifications/{id}/read
```

---

## Organizer Endpoints (role: ORGANIZER)

### Profile

```
# Get profile
GET /api/organizer/profile

# Create/update profile
POST /api/organizer/profile
{
  "organizationName": "My Events Co",
  "contactEmail": "contact@myevents.kz",
  "contactPhone": "+7 777 123 4567",
  "payoutBankName": "Kaspi Bank",
  "payoutAccountNumber": "KZ12345678",
  "payoutAccountHolder": "John Doe"
}
```

### Events

```
# Create event
POST /api/events
{
  "title": "My Event",
  "description": "Description here",
  "category": "Music",
  "venueName": "Almaty Arena",
  "venueAddress": "Some Street 123",
  "city": "Almaty",
  "eventDate": "2026-12-01T18:00:00",
  "eventEndDate": "2026-12-01T23:00:00",
  "registrationOpenDate": "2026-09-01T00:00:00",
  "registrationCloseDate": "2026-11-30T23:59:00",
  "capacity": 1000,
  "visibility": "PUBLIC"
}

# Update event
PUT /api/events/{id}

# Publish / Unpublish / Cancel / Duplicate
POST /api/events/{id}/publish
POST /api/events/{id}/unpublish
POST /api/events/{id}/cancel
POST /api/events/{id}/duplicate

# My events
GET /api/events/my
GET /api/events/my/status/DRAFT
GET /api/events/my/status/PUBLISHED
GET /api/events/my/status/COMPLETED
GET /api/events/my/status/CANCELLED
```

### Ticket Types

```
# Create ticket type
POST /api/events/{eventId}/ticket-types
{
  "name": "VIP",
  "description": "VIP access",
  "price": 15000,
  "quantity": 100,
  "free": false,
  "maxPerOrder": 5,
  "salesStart": "2026-09-01T00:00:00",
  "salesEnd": "2026-11-30T23:59:00"
}

# Update
PUT /api/ticket-types/{id}

# Hide (soft delete)
POST /api/ticket-types/{id}/hide

# Get all (including hidden)
GET /api/events/{eventId}/ticket-types/all
```

### Paid Sales Activation

```
# Check activation requirements
GET /api/events/{id}/activation-checklist

# Activate paid sales (simulated fee payment)
POST /api/events/{id}/activate
```

### Orders & Refunds

```
# View event orders
GET /api/orders/event/{eventId}

# Refund an order
POST /api/orders/{orderId}/refund
{ "reason": "Customer requested refund" }
```

### Promo Codes & Campaigns

```
# Create campaign
POST /api/promo/campaigns
{
  "eventId": 2,
  "name": "Summer Sale",
  "discountType": "PERCENTAGE",
  "discountValue": 15,
  "maxRedemptions": 50,
  "validFrom": "2026-09-01T00:00:00",
  "validTo": "2026-10-01T00:00:00"
}

# discountType: "PERCENTAGE" or "FIXED_KZT"

# View event campaigns
GET /api/promo/campaigns/event/{eventId}

# Deactivate
POST /api/promo/campaigns/{id}/deactivate
```

### Staff Management

```
# Assign event admin
POST /api/staff/assign
{ "eventId": 1, "userId": 4 }

# View event staff
GET /api/staff/event/{eventId}

# Remove staff
DELETE /api/staff/{assignmentId}
```

### Analytics

```
# Event analytics (sales, revenue, attendance, campaigns)
GET /api/analytics/events/{eventId}

# Event activity timeline (audit log)
GET /api/analytics/events/{eventId}/timeline
```

### Support (organizer side)

```
# View cases for my event
GET /api/support/cases/event/{eventId}

# Reply to a case
POST /api/support/cases/{caseId}/messages
{ "message": "We're looking into it" }

# Update case status
PATCH /api/support/cases/{caseId}/status
{ "status": "IN_PROGRESS" }
# Status options: OPEN, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED
```

---

## Event Admin / Check-In Endpoints (role: EVENT_ADMIN)

Used by the mobile ticket-verification app. Access to every endpoint below: staff assigned to the event
(`/api/staff/assign`), the event's organizer, or a platform admin. Anyone else gets `400 Not authorized for this event`.

```
# View my assigned events
GET /api/staff/my-events
```

### Scan a QR code

```
POST /api/checkin/scan
{ "ticketCode": "uuid-from-qr-code", "eventId": 1 }
```

`eventId` is optional but recommended: it is the event the staff member is working. With it, a ticket from
another event comes back as `WRONG_EVENT`; without it, the server checks access against the ticket's own event.

The response is **always 200**. Branch on `verdict`, not on `message`:

| verdict | valid | Meaning |
|---------|-------|---------|
| `OK` | true | Check-in recorded now |
| `ALREADY_USED` | false | Already checked in; `checkedInAt` / `checkedInBy` say when and by whom |
| `CANCELLED` | false | Ticket cancelled |
| `REFUNDED` | false | Ticket refunded |
| `WRONG_EVENT` | false | Ticket belongs to a different event (`eventId` / `eventTitle` in the response say which) |
| `NOT_FOUND` | false | No ticket with this code |
| `PROMO_CODE` | false | The QR is a promo/campaign code, not a ticket; see `promo` |

Response for a ticket (fields absent when not applicable):
```json
{
  "verdict": "OK",
  "valid": true,
  "message": "Check-in successful",
  "ticketCode": "4d0c...",
  "ticketId": 12,
  "orderId": 7,
  "eventId": 1,
  "eventTitle": "BiletFlow Demo Concert",
  "attendeeName": "John Doe",
  "attendeeEmail": "john@example.com",
  "ticketType": "VIP",
  "status": "CHECKED_IN",
  "seatSection": "A", "seatRow": "3", "seatNumber": "12",
  "checkedInAt": "2026-11-22T18:02:11",
  "checkedInBy": "Demo EventAdmin"
}
```

Response for a promo QR:
```json
{
  "verdict": "PROMO_CODE",
  "valid": false,
  "message": "This is a promo code, not a ticket",
  "ticketCode": "BF-LAUNCH20",
  "eventId": 2,
  "eventTitle": "Tech Conference Astana 2026",
  "promo": { "code": "BF-LAUNCH20", "campaignName": "Launch Discount",
             "discountType": "PERCENTAGE", "discountValue": 20.00, "eventId": 2, "active": true }
}
```

Duplicate scans of the same code within a few seconds are not de-duplicated server-side; the second
scan simply returns `ALREADY_USED`. Debounce on the device if needed.

### Manual check-in (no QR)

```
POST /api/checkin/tickets/{ticketId}
```
Same response shape and verdicts as `/scan`.

### Undo a check-in

```
POST /api/checkin/undo/{ticketId}
{ "reason": "Scanned the wrong person" }     # body optional
```
Response: `{ "message": "Check-in reversed", "ticketId": 12, "ticketCode": "...", "status": "VALID" }`.
The reason is stored on the check-in record and in the event audit log.

### Attendee search

```
GET /api/checkin/events/{eventId}/attendees?q=john
```
`q` matches attendee name, email, ticket code (substring, case-insensitive) or an exact order id.
Omit `q` to list every ticket of the event. Sorted by attendee name.

```json
[
  { "ticketId": 12, "ticketCode": "4d0c...", "orderId": 7,
    "attendeeName": "John Doe", "attendeeEmail": "john@example.com",
    "ticketType": "VIP", "status": "CHECKED_IN",
    "seatSection": "A", "seatRow": "3", "seatNumber": "12",
    "checkedInAt": "2026-11-22T18:02:11" }
]
```
`status` is one of `VALID`, `CHECKED_IN`, `CANCELLED`, `REFUNDED`.

### Check-in stats

```
GET /api/checkin/stats/{eventId}
```
```json
{
  "totalRegistered": 150,
  "checkedIn": 87,
  "notCheckedIn": 63,
  "cancelled": 4,
  "refunded": 2,
  "reversed": 1,
  "byTicketType": [
    { "ticketTypeId": 2, "name": "Early Bird", "registered": 100, "checkedIn": 60 },
    { "ticketTypeId": 4, "name": "VIP", "registered": 50, "checkedIn": 27 }
  ],
  "recentCheckIns": [
    { "ticketId": 12, "attendeeName": "John Doe", "ticketType": "VIP",
      "checkedInAt": "2026-11-22T18:02:11", "checkedBy": "Demo EventAdmin" }
  ]
}
```
`totalRegistered` counts VALID + CHECKED_IN tickets (cancelled and refunded excluded). `recentCheckIns` is the
last 20 non-reversed check-ins. Rejected scans are not stored server-side.

---

## Platform Admin Endpoints (role: PLATFORM_ADMIN)

```
GET /api/admin/users
GET /api/admin/events
GET /api/admin/orders
POST /api/admin/users/{id}/suspend
POST /api/admin/users/{id}/unsuspend
POST /api/admin/events/{id}/suspend
```

---

## Error Responses

All errors return:
```json
{
  "error": "Description of what went wrong",
  "timestamp": "2026-09-07T12:00:00"
}
```

Common HTTP status codes:
- `200` - Success
- `400` - Bad request / validation error / business rule violation
- `401` - Not authenticated (missing or invalid token)
- `403` - Not authorized (wrong role)
- `500` - Server error

---

## Seed Data Summary

The database comes pre-loaded with:

| What | Details |
|------|---------|
| 4 users | admin, organizer, attendee, event admin (all password: 123) |
| 2 events | Free concert (Almaty), Paid tech conference (Astana) |
| 4 ticket types | General Admission (free), Early Bird (5000 KZT), Standard (10000 KZT), VIP (25000 KZT) |
| 1 promo code | `BF-LAUNCH20` - 20% off tech conference |
| Staff assignments | Event admin assigned to both events |
| Audit logs | Event creation and publishing entries |

---

## CORS

The backend allows requests from:
- `http://localhost:3000` (React dev server)
- `http://localhost:5173` (Vite dev server)

If your frontend runs on a different port, update `SecurityConfig.java`.
