# Fix: "Failed to configure a DataSource" / "Failed to determine a suitable driver class"

## Symptom
App fails to start with:
```
Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.
Reason: Failed to determine a suitable driver class
```
Even though `application.properties` clearly has `spring.datasource.url`, `username`, `password`, and `driver-class-name` set correctly.

## Root Cause
Spring Boot reads its config from the **compiled classpath** (`target\classes\application.properties`), not directly from `src\main\resources`. If the project hasn't been built/compiled recently (or at all) by Maven, `target\classes` may not exist or may be stale — so the running app sees no datasource config at all, even though the source file is correct.

## How to Diagnose (PowerShell)

1. Confirm the source file exists and looks right:
   ```powershell
   Get-ChildItem -Recurse -Include application*.properties,application*.yml
   ```

2. Check what's actually in the compiled classpath:
   ```powershell
   Get-Content target\classes\application.properties
   ```
   - **File/folder doesn't exist** → project was never compiled by Maven. This is the fix below.
   - **File exists but is empty/different from source** → stale build, same fix applies.
   - **File matches source exactly** → config isn't the problem; check MySQL is actually running on port 3306, or check for env vars / VM options overriding the datasource (e.g. `SPRING_DATASOURCE_URL`).

## Fix

```powershell
cd "path\to\your\backend\project"
.\mvnw.cmd clean compile
```

Then verify:
```powershell
Get-Content target\classes\application.properties
```
It should now show your real datasource config.

Run the app with a **fresh start** — not a DevTools hot-reload:
```powershell
.\mvnw.cmd spring-boot:run
```
or use your IDE's run button after a full rebuild (IntelliJ: Build → Rebuild Project).

## Notes / Gotchas
- `find` is a Unix command — on Windows PowerShell, use `Get-ChildItem -Recurse -Include ...` instead. `cat` works as an alias for `Get-Content`, but combining Unix flags like `-o` won't work in PowerShell.
- `mysql-connector-j` dependency and driver-class-name were correct the whole time in this case — always rule out "config never reached the classpath" before assuming a dependency or MySQL server issue.
- If using IntelliJ's own build system (not Maven-delegated), it may output to `out\` instead of `target\` — check your IDE's build tool settings if `target\classes` still looks empty after building via the IDE.
