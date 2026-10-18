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

1. Start infrastructure: `docker compose up -d mysql kafka`
2. Start backend: `cd backend; mvn spring-boot:run`
3. Start frontend: `cd frontend; npm install; npm run dev`
4. Open `http://localhost:5173`; API base URL is `http://localhost:8080/api`.

The backend uses `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET`. To enable AI, set `AI_ENABLED=true`, `AI_MODEL_CHAT=openai`, and `OPENAI_API_KEY`. Never put production secrets in the frontend.

## First-run data and testing

Flyway creates the schema and seeds sample activities and practitioner listings. Import `backend/postman/OnlinePanditDotG.postman_collection.json` into Postman. Start with `GET /api/activities`, then register/login and create a booking. Payment is deliberately represented as `PENDING`; payment completion must be confirmed by a signed gateway webhook, not a client-supplied success flag.

## Current API

See [backend/API.md](backend/API.md) for endpoints, request bodies, roles, and the end-to-end flow. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

## Web workflows

- Visitors can filter approved practitioners by city, country, religion, and activity.
- Users can register/sign in, request a booking, view and reschedule/cancel eligible bookings, submit feedback after completion, and use the notification inbox and ceremony-planning assistant.
- Practitioners access their assigned bookings after admin approval; they can accept/decline, progress, and complete requests.
- Admins can review practitioner applications, monitor users/bookings, manage activities, and moderate feedback.
- Booking and feedback events are persisted in the in-app notification inbox and delivered over authenticated WebSockets.

Configure `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` before the first backend start to create the local administrator. Open the frontend at `http://localhost:5173` and the API at `http://localhost:8080/api`.

## Architecture and production notes

The current milestone is a modular Spring Boot deployable, not a production microservice fleet: identity, marketplace, booking, feedback, and notifications are separated by package; MySQL is the system of record and Kafka booking events are opt-in. Spring AI RAG is opt-in using `AI_ENABLED=true`, `AI_MODEL_CHAT=openai`, and `OPENAI_API_KEY`, with a Resilience4j circuit breaker around assistant requests. Independently deployed services and databases, Razorpay/Stripe, Twilio, transactional email, FCM, production hosting, CI/CD, monitoring, and mobile are not configured in this workspace and require provider accounts, infrastructure decisions, and dedicated integration tests. This project does not claim those external integrations are live.

Before production: use distinct service databases and migrations, rotate the JWT secret, enforce HTTPS, add rate limiting and audit logging, verify payment webhooks, apply privacy/retention policies, and run security/load tests.
