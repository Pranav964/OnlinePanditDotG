# OnlinePanditDotG API

Base URL: `http://localhost:8080/api`  
OpenAPI UI: `http://localhost:8080/swagger-ui.html`  
Authentication: `Authorization: Bearer <accessToken>` for protected routes.

## Local setup

Start MySQL and Kafka with `docker compose up -d mysql kafka` from the repository root. Set an initial admin account before starting the backend (password must be at least 16 characters):

```powershell
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$env:BOOTSTRAP_ADMIN_PASSWORD = "use-a-unique-password-of-16-chars"
cd backend
mvn spring-boot:run
```

Set `AI_ENABLED=true`, `AI_MODEL_CHAT=openai`, and `OPENAI_API_KEY` to enable the Spring AI assistant. Set `KAFKA_ENABLED=true` after Kafka is reachable to publish booking events to `booking-events`. Do not use local development credentials in production.

## Authentication

`POST /auth/register` creates a USER account and returns an access token.

```json
{
  "fullName": "Asha Mehta",
  "email": "asha@example.com",
  "password": "a-long-local-password",
  "phone": "+919876543210",
  "city": "Mumbai",
  "country": "India",
  "dateOfBirth": "1994-08-21"
}
```

`POST /auth/login` accepts `{"email":"asha@example.com","password":"a-long-local-password"}`. `GET /auth/me` returns the authenticated account. Roles are `USER`, `PANDIT`, and `ADMIN`; public registration cannot create elevated roles.

## Marketplace and bookings

- `GET /activities` returns active ceremony types, durations, religions, and base prices.
- `GET /pandits?city=Mumbai&country=India&religion=Hindu&activityId=1` returns approved practitioners filtered by location and specialty.
- `POST /pandits/apply` (USER token) submits a practitioner profile for admin approval. Body: `{"yearsExperience":12,"dateOfBirth":"1988-04-12","city":"Mumbai","region":"Maharashtra","country":"India","religion":"Hindu","languages":"Hindi, Sanskrit","specialties":"Griha Pravesh, Havan"}`. Applications remain `USER` until an admin approves them. Sign in again after approval to receive a JWT with the `PANDIT` role.
- `POST /bookings` (USER token) requests a booking. Price is read from the activity table, never trusted from the client.

```json
{
  "panditId": 1,
  "activityId": 1,
  "scheduledDate": "2027-02-14",
  "serviceAddress": "12 Example Road, Mumbai, Maharashtra, India"
}
```

- `GET /bookings/mine` lists the caller's bookings.
- `GET /bookings/pandit` (PANDIT token) lists bookings assigned to the caller's linked practitioner profile.
- `PATCH /bookings/{id}/cancel` (booking owner) cancels a `PENDING` or `CONFIRMED` booking. The assigned practitioner receives an in-app notification.
- `PATCH /bookings/{id}/reschedule` (booking owner) accepts `{"scheduledDate":"2027-02-16"}` for a `PENDING` or `CONFIRMED` booking and notifies the practitioner.
- `PATCH /bookings/{id}/status` (assigned PANDIT or ADMIN) accepts `{"status":"CONFIRMED"}`. Allowed transitions: `PENDING -> CONFIRMED/CANCELLED`, `CONFIRMED -> IN_PROGRESS/CANCELLED`, `IN_PROGRESS -> COMPLETED`.
- Customer and practitioner booking lists include display names, ceremony name, address, payment state, date, and amount.
- Payment remains `PENDING`; a provider order, signed webhook validation, receipt generation, and email delivery must be integrated before accepting real money.
- `POST /feedback` (booking owner, after `COMPLETED`) accepts `{"bookingId":1,"rating":5,"comment":"Clear and thoughtful ceremony."}`. Reviews remain hidden until moderation.
- `GET /pandits/{id}/feedback` returns approved reviews.

## Notifications

- `GET /notifications?unreadOnly=true&category=BOOKING&page=0&size=20` returns a page and unread count. Categories include `BOOKING`, `FEEDBACK`, and `ACCOUNT`.
- `PATCH /notifications/{id}/read` marks one item as read.
- `POST /notifications/read-all` marks all caller notifications read.
- WebSocket STOMP endpoint: `ws://localhost:8080/ws`. Send `CONNECT` native header `Authorization: Bearer <token>`, then subscribe to `/user/queue/notifications`. Only authenticated user destinations are accepted.

## Admin

All require an ADMIN token. Bootstrap admin is only created when `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` are set on first startup.

- `GET /admin/users`
- `GET /admin/bookings` (includes payment status)
- `GET /admin/pandits/pending`
- `PATCH /admin/pandits/{id}/approval?approved=true`
- `GET /admin/feedback/pending`
- `POST /admin/activities` body: `{"name":"Naming ceremony","category":"Life celebrations","duration":"2 hours","basePrice":4000,"religion":"Hindu"}`
- `PATCH /admin/feedback/{id}/moderation?approved=true`; customers and linked practitioners receive moderation/submission notifications.

## AI assistant

`POST /ai/chat` requires a user bearer token and is available only when AI is enabled and configured. Body: `{"message":"What should I discuss with a practitioner for a house-entry ceremony?"}`. The endpoint retrieves relevant local ceremony notes and supplies them as context to Spring AI. It is planning support, not a source of binding religious, legal, or medical advice.

## Error semantics

- `400`: invalid input/date/pagination.
- `401`: missing or invalid bearer token.
- `403`: authenticated account lacks the required role/ownership.
- `404`: requested record not found or not approved for public use.
- `409`: duplicate account/review or invalid booking transition.

All database timestamps use UTC. Current sample prices are INR; international currencies, tax, exchange rates, and region-specific payment providers are not yet configured. Payment remains deliberately `PENDING`: do not mark a booking paid from a browser request. A live payment gateway, signed webhook, receipt and email delivery need provider credentials and integration tests. SMS, FCM and external email are also not configured; in-app history and authenticated WebSocket updates are available locally.
