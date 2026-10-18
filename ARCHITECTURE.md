# Architecture and Project Flow

## Current runtime

```text
Browser (React + Vite :5173)
  -> REST API (Spring Boot :8080)
       -> MySQL 8 (onlinepandit)
       -> Kafka (optional booking-events producer)
       -> OpenAI via Spring AI (optional, Resilience4j circuit breaker)
        -> Stripe Checkout + Connect (test credentials, signed webhooks, configurable fee)
        -> SMTP receipt email (optional, after commit)
       -> STOMP WebSocket (/ws) for in-app notifications
```

The current backend is one deployable modular application. This is deliberate for the first end-to-end slice: booking creation, status changes, and notification persistence remain in one database transaction boundary. It is not yet a fleet of independently deployable microservices or a separate-database-per-service architecture.

## Code organization

- `frontend/src/App.jsx`: public ceremony discovery, region/tradition filters, login/register, booking request.
- `frontend/src/components/Dashboard.jsx`: customer, practitioner, and admin workspaces.
- `frontend/src/components/Notifications.jsx`: persistent inbox, unread/category filters, pagination, live WebSocket updates.
- `frontend/src/components/AIChat.jsx`: ceremony-planning chat UI.
- `backend/src/main/java/com/onlinepandit/identity`: account registration, login, JWT, optional admin bootstrap.
- `backend/src/main/java/com/onlinepandit/marketplace`: activities, region-filtered practitioner listings, approval.
- `backend/src/main/java/com/onlinepandit/booking`: booking lifecycle and optional Kafka event publication.
- `backend/src/main/java/com/onlinepandit/feedback`: completion-gated ratings and admin moderation.
- `backend/src/main/java/com/onlinepandit/notification`: user-owned history, pagination, unread count, authenticated WebSocket delivery.
- `backend/src/main/java/com/onlinepandit/ai`: optional Spring AI assistant with lightweight keyword retrieval over local planning notes.
- `backend/src/main/resources/db/migration`: forward-only Flyway schema migrations.

## Data model

`user_accounts` stores client and practitioner-login identity, role, contact/location, optional date of birth, and SMS opt-in state. `pandits` stores experience, date of birth, religion, languages, specialties, approval, service location, and Stripe Connect readiness. `activities` stores selectable ceremony type and base price. `bookings` connects user, approved practitioner, and activity with local service date/time/address, server-calculated price, booking status, and payment status. `payments` stores provider session/payment state, fee, and receipt reference. `complaints` links support issues to an owned booking/payment. `feedback` is unique per completed booking and hidden until moderated. `notifications` stores recipient, category, read state, and history. Prices use INR for this MVP.

## Booking flow

1. A visitor filters public listings by city, country, religion, and ceremony. Only approved practitioners are returned.
2. A customer registers or signs in; passwords are BCrypt-hashed and the API returns a signed JWT.
3. The customer submits practitioner, activity, date, and address. The API verifies the date and approval, calculates price from the activity record, and creates a `PENDING` booking with payment `PENDING`.
4. A persistent notification is created for the customer and (when linked to an account) practitioner. The user sees it through WebSocket and notification history. Kafka publication is opt-in.
5. An approved practitioner accepts/declines and advances the booking to in-progress/completed. Customers can cancel/reschedule only before checkout starts.
6. Approved practitioners onboard to Stripe Connect Express. Checkout creates a destination charge with a configurable fee; `PLATFORM_FEE_BPS=1000` is a development placeholder, not an approved business rate.
7. Stripe's signed webhook verifies session, amount, currency, and booking before marking paid. A PDF receipt is available and optional SMTP email sends after commit.
8. Customers can submit complaints linked to their bookings/payments; admins resolve them and status changes are notified.
9. The customer submits feedback after completion; admins publish or keep it hidden.

## Patterns and resilience

- Repository pattern: Spring Data repositories isolate persistence queries.
- DTO boundary: request and response records keep entity internals out of the REST contract.
- Role/ownership checks: JWT roles protect admin/practitioner actions; booking ownership is checked before customer mutations.
- Event-driven extension: `booking-events` is an optional Kafka topic for downstream notification/payment services.
- Circuit breaker: Resilience4j protects the optional Spring AI call and returns a user-facing fallback after provider failures.

## Recommended service split before production

1. Identity service + identity database: registration, login, accounts, roles, token issuance.
2. Marketplace service + catalog database: activities, practitioner profiles, location search, admin approval.
3. Booking service + booking database: booking lifecycle and outbox-backed booking events.
4. Payment service + payment database: provider orders, signed webhook verification, refunds, immutable transaction records, receipts.
5. Notification service + notification database: preferences, unread counters, email/SMS/FCM delivery, WebSocket fan-out.
6. AI assistant service: Spring AI, retrieval index/vector store, rate limits, provider fallback.
7. API gateway: routing, rate limiting, authentication propagation, and service-level circuit breakers.

Do not split the booking transaction by synchronous database sharing. Use Kafka events and an outbox/idempotency strategy; each service should own its database and expose APIs/events rather than foreign keys into another service's schema. This decomposition is a production follow-up, not a claim about the current runtime.

## Not enabled yet

Live Stripe processing/payouts, legal/tax setup, refunds, Twilio credentials/consent campaigns, FCM push, production SMTP, multi-currency/tax, service-specific databases, API gateway, production secrets/HTTPS, CI/CD, monitoring, load tests, and mobile app need provider accounts, infrastructure configuration, and integration tests. SMS code is opt-in and disabled until configured. Stripe and SMTP code paths are present but disabled until configured. The application intentionally does not accept a client-provided payment-success flag.
