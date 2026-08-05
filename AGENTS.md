# AGENTS.md

## Snapshot
- Stack: Spring Boot 4.0.6, Java 25, Maven wrapper, Spring MVC + Spring Data JPA, PostgreSQL, Lombok.
- Current implementation is mostly a scaffold: only `merchant` has a real persisted model (`src/main/java/com/pal/dipesh/razorpay/merchant/entity/Merchant.java`).
- Workspace-level references also exist outside the repo root: `D:\workspace\docker-compose.yml` and `D:\workspace\ER_Diagram_Code.txt`; check them when requirements or schema expectations change.
- `operations/`, `payment/`, `vault/`, `merchant/repository/`, and `common/exception/` exist as package boundaries but are currently empty.
- No prior agent-specific instructions were found from the requested glob search.

## Architecture and boundaries
- Root app entrypoint is `src/main/java/com/pal/dipesh/razorpay/RazorpayApplication.java`; package scanning starts at `com.pal.dipesh.razorpay`.
- Treat top-level packages as domain boundaries: `merchant`, `payment`, `operations`, `vault`, plus shared `common`.
- Today the codebase is entity-first, not controller/service-first: there are no controllers, services, or repositories yet.
- `D:\workspace\ER_Diagram_Code.txt` is the clearest picture of the intended system scope: merchant/admin setup (`MERCHANT`, `APP_USER`, `API_KEY`, `MERCHANT_WEBHOOK_CONFIG`, `CUSTOMER`), payment flow (`ORDER_RECORD` -> `PAYMENT` -> `REFUND` / `PAYMENT_TRANSITION_LOG`), vaulting (`VAULT_CARD`, `CARD_TOKEN`), webhook delivery (`WEBHOOK_EVENT`, `DLQ_EVENT`), and settlements (`SETTLEMENT`, `SETTLEMENT_PAYMENT`).
- The ER diagram suggests `merchant_id` is the primary tenancy boundary across most future tables; keep new persistence models aligned with that shape unless requirements explicitly change.
- If you add a feature, keep code inside its domain package rather than creating cross-cutting classes in the root package.

## Persistence patterns already in use
- `Merchant` is the reference entity for style and mapping choices.
- IDs use `UUID` with `@GeneratedValue(strategy = GenerationType.UUID)`, and `@PrePersist` also defensively assigns a UUID if null.
- Enums are stored as strings (`@Enumerated(EnumType.STRING)`), e.g. `BusinessType` and `MerchantStatus` from `src/main/java/com/pal/dipesh/razorpay/common/enums/`.
- Audit timestamps are managed in entity lifecycle hooks: `createdAt` is set once in `@PrePersist`, `updatedAt` is refreshed in both `@PrePersist` and `@PreUpdate`.
- Audit user fields (`createdBy`, `updatedBy`) are plain `String` columns right now; there is no auditing framework configured.
- Column naming uses explicit snake_case names (`contact_no`, `business_type`, `created_at`) even when Java fields are camelCase.

## Runtime and integration details
- Database configuration comes from environment variables in `src/main/resources/application.yaml`: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- JPA defaults matter: `open-in-view: false`, `hibernate.ddl-auto: update`, `show-sql: true`, and JDBC timezone is forced to `Asia/Kolkata`.
- Because `ddl-auto` is `update`, entity changes can mutate the schema automatically on startup; be deliberate with column renames and nullability changes.
- PostgreSQL is the only configured database driver in `pom.xml`; there is no embedded test database dependency.
- `D:\workspace\docker-compose.yml` provisions a local PostgreSQL 17.10 instance named `local-postgres`, exposes port `5432`, persists data in the `pgdata` volume, and sets timezone to `Asia/Kolkata`; it matches the JDBC/database details seen during test execution.

## Developer workflow verified here
- Run tests from project root with:
  ```powershell
  .\mvnw.cmd test -q
  ```
- Start the app locally with:
  ```powershell
  .\mvnw.cmd spring-boot:run
  ```
- If the local database is managed from the workspace-level compose file, the usual startup command would be:
  ```powershell
  Set-Location "D:\workspace"
  docker compose up -d postgres
  ```
- The existing test `src/test/java/com/pal/dipesh/razorpay/RazorpayApplicationTests.java` is a full `@SpringBootTest`, so it boots JPA and attempts a real datasource connection.
- In the verified environment, tests connected to PostgreSQL successfully and Spring Data reported `Found 0 JPA repository interfaces.`; expect failures if Postgres is unavailable or env vars are unset.

## Working conventions for future agents
- Use `Merchant.java` as the template when creating new entities: explicit `@Table`, explicit `@Column`, UUID primary key, and lifecycle-managed timestamps.
- Put shared enums in `common/enums`; that pattern already exists and is used by `Merchant`.
- Keep generated build output out of edits: `target/` contains compiled artifacts and copied resources, not source of truth.
- If you introduce repositories, place them under the matching domain package (for example `merchant/repository`) so component scanning stays aligned with the current layout.
- Be aware that several classes in `merchant/entity/` (`ApiKey`, `AppUser`, `Customer`, `MerchantWebhookConfig`) are placeholders with no fields or JPA annotations yet; use the ER diagram as a field/relationship reference, but treat it as a mutable design artifact rather than guaranteed truth.
- Do not copy secrets from `D:\workspace\docker-compose.yml` into committed source or docs; if you need local connection details, prefer environment variables and reference the compose file rather than duplicating credentials.

