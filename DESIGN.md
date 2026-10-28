# Design

## Data model

```mermaid
erDiagram
    supplier ||--o{ partnership : "is linked by"
    retailer ||--o{ partnership : "is linked by"
    partnership ||--o{ partnership_renewal : "keeps"
    supplier ||--o{ offer : publishes
    offer ||--o{ subscription : "is subscribed by"
    retailer ||--o{ subscription : holds
    persona ||--o{ app_user : "role of"
    supplier |o--o| app_user : "account of"
    retailer |o--o| app_user : "account of"

    supplier { bigint id; text company_name "unique, case-insensitive"; text contact_name; text contact_email; text contact_phone; char2 country_code }
    retailer { bigint id; text store_name; text store_type "ONLINE | PHYSICAL | HYBRID"; char2 country_code; text city; text address; text website; text contact_email }
    partnership { bigint id; date start_date; date end_date; date terminated_on; text termination_reason }
    partnership_renewal { bigint id; date renewed_on; date previous_end_date; date new_end_date }
    offer { bigint id; text reference "unique per supplier"; text title; text description; text discount_type; numeric discount_value; char3 currency; int minimum_order_quantity; date valid_from; date valid_to; text status "DRAFT | PUBLISHED | CLOSED" }
    subscription { bigint id; date subscribed_on; date cancelled_on; text cancellation_cause; bigint supplier_id "copied from the offer" }
    audit_entry { bigint id; timestamptz occurred_at; text actor; text subject_type; bigint subject_id; text action; text details }
```

### Partnership status is computed, not stored

The `partnership` table has no status column. The status (SCHEDULED, ACTIVE, EXPIRED or TERMINATED) is derived from the dates and `terminated_on`, relative to today in Europe/Paris:

- in Java, by `Partnership.status(today)`;
- in SQL, for the status filter and for "expiring within N days".

This makes BR-05 true by construction: no job has to flip a partnership to EXPIRED.

A subscription's *Suspended* state works the same way (BR-12). It is never stored: a subscription that is not cancelled reads as Suspended while its partnership is not ACTIVE. It reads as Active again as soon as the partnership is renewed.

### Database rules that back up the domain

The domain checks every rule first, so callers get a clear business error code. The database repeats what it can express:

| Rule  | Database guard                                                                                 |
| ----- | ---------------------------------------------------------------------------------------------- |
| BR-01 | unique index on `lower(company_name)`                                                          |
| BR-02 | `CHECK` constraint on the location columns per store type                                      |
| BR-03 | `end_date > start_date`                                                                        |
| BR-04 | a GiST `EXCLUDE` constraint on overlapping periods for the same pair, ignoring terminated rows |
| BR-09 | discount and validity checks                                                                   |
| BR-11 | a partial unique index on `(retailer_id, offer_id)` where the subscription is not cancelled    |

## Backend architecture

The backend is a modular monolith built with Spring Modulith, under `com.equadis.portal`:

- `shared`: the shared kernel. It holds `BusinessException` and `ErrorCode`, the value objects (`DateRange`, `Email`, `CountryCode`), the current actor, and the global error handler.
- `modules.{supplier, retailer, partnership, offer, subscription, audit, notification, auth, persona}`: one module per business area.

Each module has the same four layers, a simple DDD layering:

| Layer            | What it holds                                                                                                                                                                      |
| ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `domain`         | Aggregates that enforce their own rules (`Partnership.renew/terminate`, `Offer.publish/close`, `Subscription.cancel`) and the repository ports. No Spring, no HTTP.              |
| `application`    | Services that run a use case in one transaction, take command records and publish domain events.                                                                                  |
| `infrastructure` | Package-private Spring Data adapters that implement the ports, and `*ReadModel` classes for queries. Read and write sides stay apart in a light CQRS style.                        |
| `web`            | Controllers and request/response DTOs. Entities are never returned.                                                                                                                |

**How modules talk to each other.** A module calls another only through its root package: a small `*Queries` interface (for example `PartnershipQueries.hasActivePartnership`), or events. Examples:

- **Synchronous event listeners, same transaction:**
  - `PartnershipTerminated` makes the subscription module cancel the matching subscriptions (BR-07).
  - `OfferClosed` does the same for the closed offer (BR-08).

  So a termination or close and its cancellations commit or roll back together.
- **After-commit listeners, separate transaction:** the audit log records partnership and offer events after the commit, in its own transaction. An audit failure never undoes the business action.

**Tests that enforce the structure:**

- `ModularityTest` (Spring Modulith) fails on cyclic or undeclared dependencies between modules.
- `ArchitectureTest` (ArchUnit) enforces the layering and placement rules: the domain has no Spring, the web layer never uses repositories, controllers live in `web`, commands are records, there is no field injection, and so on.

**Errors.** Every error is an RFC 9457 `ProblemDetail` with a `code` (for example `PARTNERSHIP_OVERLAP`) and, for validation, `fieldErrors`. The frontend translates the codes.

**Dates.** A `Clock` bean in Europe/Paris is the single source of "today". Tests can fix the date with it.

**Security (bonus).**

- Stateless JWT (HS256) with the claims `role`, `party_id` and `can_act_as`. Passwords are hashed with bcrypt.
- `ActorAccess` makes sure a supplier or retailer only reaches its own data.
- "Acting as" re-issues a token for the chosen persona. Only accounts with `can_act_as` (the administrator) may do this. They keep access to the selector lists while acting as someone else.

**Expiry job (bonus).**

- Runs every day at 08:00 Europe/Paris.
- Finds the partnerships ending in exactly 30, 7 or 1 days and sends one notice per milestone through a `Notifier` port.
- The adapter only logs. An e-mail or Slack adapter can replace it without changing the job.

## Frontend architecture

Three top-level folders. ESLint (`eslint-plugin-boundaries`) enforces the import rules between them:

- `shell/`: the layout, the top bar and the menu per persona.
- `domains/<name>/`: one folder per business area, holding `models/`, `services/`, `pages/`, `components/` and `<name>.routes.ts`. A domain never imports another domain.
- `shared/`: auth, HTTP, i18n, notifications, date helpers and UI helpers. Anything may import from it.

The other conventions:

- **One service per domain.** It holds both the API calls and the state, as signals plus `rxResource`.
- **Signals and OnPush everywhere.** The app is zoneless.
- **Injected private services** are named with a leading `_`.
- **The UI follows the brief's pattern:** a table, a "New" dialog, and row actions shown only when the API says the action is allowed. Partnerships and offers return `allowedActions`, so the UI never re-implements the rules.
- **PrimeNG theme:** the Aura preset with primary `#58EA9D` and navy `#081E32` for the top bar and table headers.
- **Translations:** Transloco for English and French. PrimeNG's own texts (date picker, paginator) switch language too.

## Assumptions

**Dates**

- Every date is a calendar date in Europe/Paris, on both backend and frontend.
- Partnership periods and offer validity windows include both ends.
- A partnership is ACTIVE from its start date through its end date, and EXPIRED from the day after.

**Partnerships (BR-03, BR-06, BR-07)**

- BR-03: "lasts at most 36 months" means `end date ≤ start date + 36 months`.
- BR-06, renewal:
  - The new end date must be after the current one.
  - The 36-month limit is counted from the current end date, or from today if the partnership has already expired.
  - "Expired for at most 30 days" means today ≤ end date + 30 days.
- BR-07, termination:
  - Allowed only for a SCHEDULED or ACTIVE partnership.
  - Takes effect the same day.
  - Cancels the retailer's non-cancelled subscriptions to that supplier's offers, with the cause `PARTNERSHIP_TERMINATED`.

**Offers (BR-08, BR-10)**

- BR-08:
  - An offer can be published only while its validity window has not ended.
  - Closing is allowed only for a PUBLISHED offer. It cancels the offer's subscriptions with the cause `OFFER_CLOSED`.
  - A draft is never subscribed to, so a draft cannot be closed.
- An offer reference is unique per supplier, ignoring case.
- BR-10 applies when subscribing as well as when listing. A published offer whose validity starts tomorrow is not yet visible.

**Subscriptions (BR-11, BR-12)**

- BR-11: a retailer who cancelled can subscribe to the same offer again. Only one *non-cancelled* subscription is allowed.
- BR-12: a suspended subscription can still be cancelled by the retailer. A new subscription needs an ACTIVE partnership (BR-10).

**Who can do what**

- **Deleting** a supplier or retailer that still has partnerships, offers or subscriptions is refused (`SUPPLIER_IN_USE` / `RETAILER_IN_USE`). History is never deleted silently.
- **The administrator** can do everything, including a supplier's or retailer's actions.
- **Suppliers and retailers** only reach their own data.
- **Sign-up** creates a new supplier or retailer company and its account in one step.

## AI usage

This project was built with **Claude Code** (Anthropic's Claude) as a pair programmer, working inside the repository.

**What I decided:**

- the architecture and the conventions:
  - `shell` / `domains` / `shared` on the frontend, with no imports between domains;
  - one service per domain, which holds both the API calls and the state;
  - the `_` prefix for private injected services;
  - business modules grouped under `modules` with Spring Modulith;
- which bonus features to build;
- how ambiguous rules should behave.

**What Claude did:** proposed designs, wrote most of the code, tests and documentation, and fixed the issues found while reviewing.

**How everything was checked:** I reviewed the work as it progressed. Every change was checked with:

- the backend test suite (unit, architecture and integration tests);
- the frontend lint, tests and build;
- a manual walkthrough of each persona in the browser.
