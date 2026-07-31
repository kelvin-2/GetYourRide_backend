# Project Rules

Before beginning any task:

1. Read `/docs/architecture.md`.
2. Read `/docs/tasks.md`.
3. Use the architecture as the source of truth.
4. Complete only the current phase from the task roadmap.
5. Do not begin the next phase until the current phase satisfies all acceptance criteria.

If documentation and implementation disagree:

- Report the discrepancy.
- Do not invent a solution.
- Ask for clarification.

Never replace existing architecture without approval.

All new features must align with the documented architecture.
# Project Rules — GetYourRide Backend

Grounded in what the codebase actually does today, plus where it should converge going forward. Where current code is inconsistent, that's called out explicitly rather than papered over.

## Architecture & Layering

- Strict layering: `controller` → `service` (interface) → `service/impl` (implementation) → `repository`. Controllers never touch repositories directly.
- Every service is an interface + a separate `XxxServiceImpl`, even for simple CRUD. This is followed consistently in the current code — keep it that way.
- Constructor injection only. No `@Autowired` field injection anywhere in the codebase, and no exceptions going forward.
- DTOs at the boundary: `dto/request/XxxRequest` in, `dto/response/XxxResponse` out. Entities are never returned directly from a controller.

## Entities: Lombok vs. manual — pick one going forward

The codebase is currently **split**:
- `Driver`, `Student`, `Trip`, `Vehicle`, `DriverApplication` use Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, sometimes `@Builder`).
- `ShuttleStop`, `ShuttleTimeSlot`, `TripStop` use manual getters/setters, no Lombok.

**Rule going forward: no new Lombok usage.** Write manual getters/setters/constructors on any new or touched entity, matching the `ShuttleStop`/`TripStop` style. Don't retrofit the existing Lombok entities unless you're already modifying that file for another reason — not worth a pure refactor PR.

## Code style

- Complete, paste-ready file rewrites are preferred over partial diffs/snippets when editing a file substantially.
- Exceptions are explicit domain types (`BadRequestException`, `ResourceNotFoundException`) caught centrally by `GlobalExceptionHandler` — don't introduce ad hoc exception handling inside a controller or service method.
- Status fields (`Trip.status`, `DriverApplication.applicationStatus`) are currently plain `String`, not enums, except `ShuttleTimeSlot.period` which is already an enum (`Period`). New status-like fields should be enums; existing string-status fields can be left alone unless a phase specifically calls for the enum migration (see `doc/Task` Phase 0, which already enforces `trip.status` as an ENUM at the DB level).

## Security rules

- All new endpoints are authenticated by default (`anyRequest().authenticated()` in `SecurityConfig`). Only add a route to the public matcher list (`/api/auth/**`, `/api/driver-applications/**`, `/error`) if it must be reachable pre-login — state the reason when doing so.
- Never add new secrets to `application.properties` in plaintext. The file already has committed secrets (DB password, ORS key, Geoapify key, Cloudinary secret) — this is a known, flagged issue, not a pattern to extend. New secrets go through environment variables; if asked to "add an API key," ask where it should be sourced from rather than defaulting to the properties file.
- `Student.password` is currently stored in plain text, noted in `doc/ARCHITECTURE.md` as "per project decision." Treat this as a flagged risk, not a convention to imitate for any new credential field.
- `@PreAuthorize`/role checks: `/api/admin/**` is reserved for `ADMIN`/`STAFF` roles at the security-config level, but no admin controller exists yet. If admin endpoints are added, use this existing role gate rather than inventing a new one.

## API conventions

- Path style: `/api/{resource}` (plural nouns: `trips`, `vehicles`, `shuttle-stops`), nested sub-resources under their parent (`/api/trips/{tripId}/stops`). Follow this instead of flat top-level routes for anything that belongs to a trip.
- Status/action transitions on a resource use `PATCH /{id}/{action}` (e.g. `/api/trips/{id}/cancel`, `/complete`, `/schedule`) rather than a generic `PUT` with a body — follow this pattern for any new trip-like state transition.
- Errors return through `GlobalExceptionHandler`; don't build a new per-controller error shape.

## Database & migrations

- `spring.jpa.hibernate.ddl-auto=update` is currently used for schema management (no separate migration tool like Flyway/Liquibase is present). Be cautious with entity field changes — `update` can silently leave stale columns. If a phase needs a destructive change (rename/drop), call it out explicitly rather than relying on `ddl-auto` to handle it safely.
- `doc/Task` Phase 0 introduces raw SQL migration files (e.g. `01_cleanup_and_simulation_schema.sql`) run manually against a DB copy — this is the established pattern for anything `ddl-auto` can't safely express (enums, backfills, dedup). Follow it for future manual migrations rather than fighting Hibernate auto-DDL into doing it.

## Testing

- `spring-boot-starter-test` and `spring-security-test` are present as dependencies; `src/test` exists but is comparatively small relative to `src/main`. Add tests for new service-layer logic, especially anything with validation branching (the Phase 1 coordinate-validation work explicitly calls for a regression test) — don't treat tests as optional for bug-fix work.

## Documentation

- `doc/ARCHITECTURE.md` describes the layered structure and current entity/endpoint set — keep it in sync when entities or endpoints change; it's a stale doc otherwise.
- `doc/Task` is the authoritative build plan for live tracking — update its checkboxes/status as phases complete rather than tracking progress elsewhere.

## Things to flag rather than silently fix or ignore

- Hardcoded placeholder coordinates in `RouteController` (known, tracked in Phase 2 — don't "fix" it outside that phase's scope without noting it).
- Plaintext secrets in `application.properties`.
- Plaintext password storage on `Student`.
- Mixed Lombok/manual entity style (documented above — not a bug, just be consistent going forward).
