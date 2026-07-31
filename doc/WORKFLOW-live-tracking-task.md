# Workflow — Executing the Live Tracking Build Plan

This defines *how* to work through `doc/Task` (the phased live tracking + simulation plan), not what to build — that's already specified there. Follow this process for every phase.

## Before touching any phase

1. Read `doc/Task` in full and locate the specific phase you're starting.
2. Read the matching section of `doc/GetYourRide_Tracking_Documentation.md` for architectural context.
3. Confirm the previous phase's acceptance criteria are actually checked off in `doc/Task` — do not start a phase whose prerequisite isn't done, even if it looks safe to skip ahead.
4. State which phase you're starting and its acceptance criteria back before writing code, so scope doesn't drift mid-phase.

## Per-phase loop

1. **Branch**: create/use the branch named in the plan (e.g. `step-2-route-wiring`). Do not combine two phases into one branch.
2. **Implement** only the deliverables listed for that phase. If you notice unrelated bugs or improvements, note them at the end of your response — don't fold them into the same change.
3. **Run the "what to check first" list** in `doc/Task` for that phase before declaring it done. These are pre-merge sanity checks, not optional.
4. **Verify every acceptance criterion** for the phase explicitly, one by one. If one can't be verified (e.g. no local DB access), say so rather than marking it done anyway.
5. **Update `doc/Task`**: tick the completed checkboxes and flip the phase's status to ✅ Done. Leave a one-line note if anything deviated from the original plan (e.g. a schema field renamed).
6. **Stop at the phase boundary.** Don't continue into the next phase in the same turn unless explicitly asked — each phase is meant to be independently reviewable.

## Known blockers to flag, not silently work around

- **Plaintext secrets in `application.properties`** (DB password, ORS key, Geoapify key, Cloudinary secret) — if a phase touches config, flag that these should move to environment variables rather than adding to the plaintext file.
- **`Student.password` stored in plain text** — if a phase touches auth or student data, flag this rather than assuming it's intentional and permanent.
- **The 0,0 coordinate bug (Phase 1)** — if later phases depend on clean coordinate data and Phase 1 isn't actually verified done, stop and say so rather than building Phase 2+ on top of possibly-bad data.

## Commit / PR conventions

- Commit messages reference the phase: `step-2: wire RouteController to real trip data`.
- One logical change per commit where practical (schema change, then code change, then test).
- Don't merge a phase branch to `master` until its acceptance criteria are all checked — surface this explicitly rather than assuming merge is implied by "done."

## When a phase reveals the plan itself needs to change

If implementation reveals the original phase plan is wrong or incomplete (e.g. Phase 2 needs a schema field the plan didn't anticipate), update `doc/Task` to reflect the real plan and say so — don't quietly diverge from the written plan without updating it, since it's the shared source of truth.
