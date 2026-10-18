# OnlinePanditDotG

A local-first web platform for discovering verified spiritual practitioners and requesting ceremony bookings. The repository is organized as a React client and Spring Boot backend; provider integrations are configured by environment variables and are not enabled by default.

## Project layout

- `frontend/` React + Vite single-page application.
- `backend/` Spring Boot REST API, MySQL schema/migrations, and API docs.
- `docker-compose.yml` local MySQL and Kafka infrastructure.

## Prerequisites

- Java 21 and Maven 3.9+
- Node.js 20+ with npm
- Docker Desktop (for MySQL/Kafka), or a local MySQL 8 instance

## Local run

### Docker (recommended)

With Docker Desktop running, from the repository root run `docker compose up --build`. Then open the React app at `http://localhost:5173`, the REST API at `http://localhost:8080/api`, and Swagger at `http://localhost:8080/swagger-ui.html`. Compose includes local-only MySQL, Kafka, and bootstrap admin credentials; override them before sharing the setup or using a public environment.

### Run services separately

1. Start infrastructure: `docker compose up -d mysql kafka`
2. Start backend: `cd backend; mvn spring-boot:run`
3. Start frontend: `cd frontend; npm install; npm run dev`
4. Open `http://localhost:5173`; API base URL is `http://localhost:8080/api`.

The backend uses `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`. Stripe test checkout uses `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`, and a separate `STRIPE_CONNECT_WEBHOOK_SECRET`; forward both webhook types with Stripe CLI. `PLATFORM_FEE_BPS=1000` is a 10% placeholder. AI uses `AI_ENABLED=true`, `AI_MODEL_CHAT=openai`, and `OPENAI_API_KEY`. Receipt email is optional via `MAIL_ENABLED=true` and SMTP settings. Twilio SMS uses `SMS_ENABLED=true`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, and `TWILIO_FROM_NUMBER`; reminders additionally require `SMS_REMINDERS_ENABLED=true`. Never put production secrets in the frontend.

## First-run data and testing

Flyway creates the schema and seeds sample activities and practitioner listings. Import `backend/postman/OnlinePanditDotG.postman_collection.json` into Postman. Start with `GET /api/activities`, then register/login and create a booking. Payments are confirmed only by a verified Stripe webhook, not a browser redirect or client-supplied success flag.

## Current API

See [backend/API.md](backend/API.md) for endpoints, request bodies, roles, and the end-to-end flow. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Web workflows

- Visitors can filter approved practitioners by city, country, religion, and activity.
- Users can register/sign in, request a booking, view and reschedule/cancel eligible bookings, submit feedback after completion, and use the notification inbox and ceremony-planning assistant.
- Practitioners access their assigned bookings after admin approval; they can accept/decline, progress, and complete requests.
- Admins can review practitioner applications, monitor users/bookings/payments, manage activities, moderate feedback, and resolve support complaints.
- Booking and feedback events are persisted in the in-app notification inbox and delivered over authenticated WebSockets.

Configure `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` before the first backend start to create the local administrator. Open the frontend at `http://localhost:5173` and the API at `http://localhost:8080/api`.

## Architecture and production notes

The current backend is one modular Spring Boot deployable, not a production microservice fleet. It has functional test-mode Stripe Checkout/Connect code, signed webhooks, destination-charge fees, PDF receipts, optional SMTP and opt-in Twilio delivery; each provider remains disabled until credentials and merchant settings are configured. MySQL is the system of record and Kafka booking events are opt-in. Spring AI RAG is opt-in using `AI_ENABLED=true`, `AI_MODEL_CHAT=openai`, and `OPENAI_API_KEY`, with a Resilience4j circuit breaker. Service-owned databases, API gateway, FCM, refunds, global tax/currency, production hosting, CI/CD, monitoring, and mobile still require separate implementation, accounts, and integration testing.

Before production: use distinct service databases and migrations, rotate the JWT secret, enforce HTTPS, add rate limiting and audit logging, verify payment webhooks, apply privacy/retention policies, and run security/load tests.
