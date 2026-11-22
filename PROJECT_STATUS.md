# BiletFlow Backend - Project Status

## Tech Stack
- **Framework:** Spring Boot 4.1.1 (Java 25)
- **Database:** PostgreSQL
- **Auth:** JWT (jjwt 0.12.6)
- **ORM:** Spring Data JPA / Hibernate
- **QR Codes:** ZXing 3.5.3
- **PDF Tickets:** OpenPDF 2.0.3
- **API Docs:** Swagger / SpringDoc OpenAPI (http://localhost:8080/swagger-ui.html)
- **Security:** Spring Security with role-based access
- **Migrations:** Flyway
- **Build:** Maven

## What's Done (Backend)

### Entities (16 JPA models)
- User, OrganizerProfile, Event, TicketType, Order, OrderItem, Ticket
- Payment, Refund, CheckInRecord, StaffAssignment
- SupportCase, SupportMessage
- PromotionalCampaign, PromoCode
- AuditLog, Notification

### Enums (8)
- Role, EventStatus, EventVisibility, OrderStatus, TicketStatus
- PaymentStatus, SupportCaseStatus, DiscountType

### Repositories (16)
- Spring Data JPA interfaces for all entities with custom query methods

### Security
- JWT token generation and validation
- JWT auth filter on every request
- Role-based access control (ATTENDEE, ORGANIZER, EVENT_ADMIN, PLATFORM_ADMIN)
- CORS configured for frontend dev servers
- Password hashing with BCrypt
- Swagger endpoints whitelisted

### Services (11)
| Service | What it does |
|---------|-------------|
| AuthService | Register, login, email verification |
| EventService | Full CRUD, publish/unpublish/cancel/duplicate events |
| TicketTypeService | Create/edit/hide ticket types per event |
| OrderService | Create orders, payment, refund, cancel, inventory management |
| CheckInService | QR scan, validate, check-in, undo, stats |
| PromoService | Create campaigns, generate promo codes, validate |
| SupportService | Create cases, message threads, assign, status updates |
| AnalyticsService | Revenue, sales, attendance, campaign stats |
| ActivationService | Paid sales activation checklist + simulated activation |
| QrCodeService | Generate QR code images (ZXing) |
| PdfTicketService | Generate printable PDF tickets with QR codes |

### Controllers (15 REST endpoints)
| Controller | Base Path | Key Endpoints |
|-----------|-----------|---------------|
| AuthController | /api/auth | POST /register, /login, GET /verify |
| EventController | /api/events | CRUD, /publish, /cancel, /duplicate |
| TicketTypeController | /api/events/{id}/ticket-types | Create, edit, hide |
| OrderController | /api/orders | POST create, /pay, /refund, /cancel |
| TicketController | /api/tickets | GET /my, /{id}/qr, /{id}/pdf |
| CheckInController | /api/checkin | POST /scan, /undo/{id}, GET /stats |
| PromoController | /api/promo | POST /campaigns, GET /validate/{code} |
| SupportController | /api/support | POST /cases, /messages, PATCH /status |
| AnalyticsController | /api/analytics | GET /events/{id}, /events/{id}/timeline |
| ActivationController | /api/events | GET /{id}/activation-checklist, POST /{id}/activate |
| StaffController | /api/staff | POST /assign, DELETE /{id}, GET /event/{id} |
| AdminController | /api/admin | GET /users, /events, /orders, POST /suspend |
| OrganizerProfileController | /api/organizer/profile | GET, POST |
| NotificationController | /api/notifications | GET, /unread, POST /{id}/read |
| CalendarController | /api/events | GET /{id}/calendar (.ics download) |

### Database Migrations (Flyway)
- **V1__create_tables.sql** — Creates all 16 tables with proper foreign keys and constraints
- **V2__seed_data.sql** — Seeds initial demo data (runs once, skipped on restart)

### Seed Data (V2)
| Email | Role | Password |
|-------|------|----------|
| admin@biletflow.kz | PLATFORM_ADMIN | 123 |
| organizer@biletflow.kz | ORGANIZER | 123 |
| attendee@biletflow.kz | ATTENDEE | 123 |
| eventadmin@biletflow.kz | EVENT_ADMIN | 123 |

- 2 demo events (1 free concert in Almaty, 1 paid tech conference in Astana)
- 4 ticket types (General Admission free, Early Bird 5000 KZT, Standard 10000 KZT, VIP 25000 KZT)
- Organizer profile with identity/payout verified
- Staff assignments for event admin on both events
- Promo code `BF-LAUNCH20` (20% off tech conference, max 100 redemptions)
- Audit log entries for event creation/publishing/activation

### Other
- Global exception handler (clean JSON error responses)
- Swagger/OpenAPI with JWT auth support (http://localhost:8080/swagger-ui.html)
- JSON circular reference fixes (@JsonIgnoreProperties, @JsonBackReference)
- Sensitive fields (password, tokens) excluded from JSON responses
- Java 25 required (project-level only, system Java can stay at 11)

## SRS Feature Coverage

| SRS Feature | Status |
|------------|--------|
| 4.1 Account Management | DONE - register, login, verify, roles |
| 4.2 Event Management | DONE - full CRUD, publish, cancel, duplicate |
| 4.3 Ticket Management | DONE - free/paid types, inventory, hide |
| 4.3.1 Assigned Seating | PARTIAL - data model ready, no seat map UI |
| 4.4 Free Registration | DONE - zero-total auto-complete |
| 4.5 Paid Sales Activation | DONE - checklist + simulated activation |
| 4.6 Checkout and Payments | DONE - simulated payment, inventory reserve |
| 4.7 Digital Tickets | DONE - QR codes + PDF generation |
| 4.8 Mobile Check-In | DONE - scan, validate, undo, stats API |
| 4.9 Orders/Cancellations/Refunds | DONE - full flow with audit |
| 4.10 Notifications | PARTIAL - model + API ready, email sending not wired |
| 4.11 Calendar Export | DONE - .ics file generation |
| 4.12 Administration | DONE - search, suspend users/events |
| 4.13 Support Chat | DONE - cases, messages, status, assign |
| 4.14 Promo Codes/Campaign QR | DONE - campaigns, codes, validation, redemption |
| 4.15 Basic Organizer Analytics | DONE - revenue, sales, attendance, campaigns |
| 4.16 Event History/Audit Trail | DONE - timeline, audit logs |

## What's Left

### Must Do Before Running
1. **Create PostgreSQL database:** `CREATE DATABASE biletflow;`
2. **Update application.properties:** Set your real DB username/password
3. **Compile and test:** `./mvnw spring-boot:run`

### Nice to Have (Not Blocking MVP)
- Wire up actual email sending (SMTP) for notifications
- Seat map data model (venue sections/rows/seats entities) for assigned seating
- File upload for event images (currently just stores URL string)
- Attendee search endpoint for check-in staff
- Rate limiting on auth endpoints
- Docker compose file for deployment

## How to Run

```bash
# 1. Make sure PostgreSQL is running and create the database
psql -U postgres -c "CREATE DATABASE biletflow;"

# 2. Update src/main/resources/application.properties with your DB credentials

# 3. Run the app (uses Java 25, system Java stays at 11)
JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home ./mvnw spring-boot:run

# 4. Open Swagger UI
# http://localhost:8080/swagger-ui.html

# 5. Login with a demo account
# POST http://localhost:8080/api/auth/login
# Body: { "email": "organizer@biletflow.kz", "password": "123" }

# 6. Copy the JWT token from the response and click "Authorize" in Swagger UI
# Paste: Bearer <your-token>
```
