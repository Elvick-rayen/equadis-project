# Supplier & Retailer Partnership Portal

The portal manages suppliers, retailers and the partnerships between them. Suppliers publish commercial offers, and retailers with an active partnership can subscribe to them.

- **Backend:** Spring Boot 4 on Java 21, PostgreSQL 17 with Flyway, and Spring Modulith.
- **Frontend:** Angular 21 with PrimeNG 21 (Aura preset in the Equadis colours) and Transloco for English and French.

Design choices, the data model and assumptions are in [DESIGN.md](DESIGN.md).

## Run everything with Docker

Requires Docker Desktop.

```bash
docker compose up -d --build
```

Open **http://localhost:8081**. The first build takes a few minutes, because Maven and npm download their dependencies.

| Container          | Role                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| `equadis-postgres` | PostgreSQL 17, port 5432, database/user/password `equadis_portal` / `portal` / `portal` |
| `equadis-backend`  | the API, `dev` profile, so the demo data is loaded                   |
| `equadis-frontend` | nginx serving the Angular build and proxying `/api` to the backend   |

Stop the app with `docker compose down`. Add `-v` to also delete the database volume, which reloads the seed data on the next start.

## Accounts

| Username   | Password   | Persona                                      |
| ---------- | ---------- | -------------------------------------------- |
| `admin`    | `admin`    | Administrator; can use **Acting as**         |
| `supplier` | `12345678` | Supplier · Alpina Foods                      |
| `retailer` | `12345678` | Retailer · City Market Lyon                  |

- **Acting as:** the administrator can act as any supplier or retailer through the selector in the top bar. Supplier and retailer accounts always act as their own company.
- **Sign up:** creates a new supplier or retailer company together with its account.

## Run for development

Requires JDK 21, Node 22 and Docker (for the database).

```bash
docker compose up -d postgres
```

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm ci && npm start
```

- **Frontend:** http://localhost:4200. The dev server proxies `/api` to `localhost:8080`.
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Windows:** use `.\mvnw.cmd` instead of `./mvnw`.

The `dev` profile is the default. It loads the seed data (`db/seed`) and creates the demo accounts.

Any other profile refuses to start without a `JWT_SECRET` environment variable (32+ bytes).

## Tests

```bash
cd backend && ./mvnw verify
```

- **Unit tests:** the business rules (BR-01 to BR-12) on the domain objects.
- **Architecture rules:** ArchUnit checks layering and naming (`ArchitectureTest`); Spring Modulith checks module boundaries (`ModularityTest`).
- **Integration tests:** HTTP → service → PostgreSQL. They need Docker, because Testcontainers starts its own PostgreSQL.

```bash
cd frontend && npx ng lint && npx ng test --watch=false && npx ng build
```

Lint also enforces the import boundaries: a domain never imports another domain.

## CI

`.github/workflows/ci.yml` runs on every push to `main` and on every pull request:

- the backend `mvnw verify`;
- the frontend lint, tests and production build;
- a final `docker compose build`.

## Seed data worth trying

Dates in the seed are relative to the day the database is created, so every status is visible on any day:

- partnerships that are SCHEDULED, ACTIVE, EXPIRED (some within the 30-day renewal window) and TERMINATED;
- offers that are DRAFT, PUBLISHED and CLOSED;
- subscriptions that are active, suspended (the partnership expired) and cancelled for each cause.

## Project layout

```
backend/   Spring Boot API: com.equadis.portal.{shared, modules.<module>.{domain,application,infrastructure,web}}
frontend/  Angular app: src/app/{shell, domains/<domain>, shared}
docker-compose.yml, .github/workflows/ci.yml
```
