# GetYourRide — Database Setup Documentation

## Overview

The `shuttle_db` MySQL database has been migrated from a local machine to a
free, cloud-hosted **TiDB Cloud** instance (MySQL-compatible), so the backend
can be deployed to Render without needing a local database running.

---

## Where the Database Is Hosted

| Property | Value |
|---|---|
| Provider | TiDB Cloud |
| Plan | Starter (free tier) |
| Instance Name | `getyourride-db` |
| Cloud Provider | AWS |
| Region | Frankfurt (eu-central-1) |
| TiDB Version | v8.5.3 |
| Database Name | `shuttle_db` |
| Host | `gateway01.eu-central-1.prod.aws.tidbcloud.com` |
| Port | `4000` |
| Username | `4Bos9C2srKVNsaB.root` |
| Connection Type | Public endpoint, TLS required |

**Free tier limits:** up to 5 GiB row storage, 5 GiB columnar storage, and 50
million Request Units/month per instance — no credit card required, and
throttles rather than bills if exceeded.

---

## What We Did (Migration Steps)

1. Created a TiDB Cloud account and provisioned a **Starter** (free) cluster
   named `getyourride-db`, region Frankfurt.
2. Created the `shuttle_db` database inside the cluster via the SQL Editor.
3. Generated a connection password via **Connect → Generate Password**.
4. Exported the local MySQL database as a dump (`getYourRide.sql`).
5. **Fixed a TiDB-specific issue**: TiDB validates foreign key references at
   parse time (unlike MySQL, which allows forward references when
   `FOREIGN_KEY_CHECKS=0`). The original dump created tables in alphabetical
   order, so tables like `boarding_log` referenced tables (`trip_booking`)
   that didn't exist yet — this caused `Failed to open the referenced table`
   errors.
   - **Fix**: stripped all 11 `CONSTRAINT ... FOREIGN KEY` clauses out of the
     `CREATE TABLE` statements, and re-added them as `ALTER TABLE ... ADD
     CONSTRAINT` statements at the very end of the script, after every table
     already exists.
6. Ran the corrected script (`getYourRide_tidb.sql`) in the TiDB SQL Editor.
7. Verified success:
   - `SHOW TABLES;` → all 12 tables present.
   - Row counts per table matched the original local dump exactly.
   - `information_schema.TABLE_CONSTRAINTS` confirmed all 11 foreign keys
     were successfully re-added.
8. Updated Spring Boot's `application.properties` to point at TiDB instead
   of local MySQL, and confirmed a successful connection via the app's
   startup logs (Hikari pool connected, Hibernate successfully queried the
   `student` table over the network).

---

## Old Way — Local MySQL (before migration)

```properties
spring.application.name=GetYourRide

spring.datasource.url=jdbc:mysql://127.0.0.1:3306/shuttle_db
spring.datasource.username=root
spring.datasource.password=Admin1234!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
logging.level.org.springframework.security=DEBUG

ors.api.key=eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjVjYjRjNzYzYjljODRlZWE5NWU0M2YyY2Y3NWMwZDA1IiwiaCI6Im11cm11cjY0In0=

# Geoapify API Key
geoapify.api.key=5b203f6b8e664a33b2f195b69f16544c

server.port=8080
```

Only worked when a local MySQL server was running on the same machine as the
Spring Boot app — not usable for a Render deployment.

---

## New Way — TiDB Cloud (current)

```properties
spring.application.name=GetYourRide

spring.datasource.url=jdbc:mysql://gateway01.eu-central-1.prod.aws.tidbcloud.com:4000/shuttle_db?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3
spring.datasource.username=4Bos9C2srKVNsaB.root
spring.datasource.password=2WBpDoD567FHjqYB
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
logging.level.org.springframework.security=DEBUG

ors.api.key=eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjVjYjRjNzYzYjljODRlZWE5NWU0M2YyY2Y3NWMwZDA1IiwiaCI6Im11cm11cjY0In0=

# Geoapify API Key
geoapify.api.key=5b203f6b8e664a33b2f195b69f16544c

server.port=8080
```

**Key differences from the old config:**
- `spring.datasource.url` now points to the TiDB Cloud public endpoint on
  port `4000` instead of `127.0.0.1:3306`, and requires SSL params
  (`sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`) — TiDB
  Cloud rejects unencrypted connections.
- `spring.datasource.username` uses TiDB's `<cluster-id>.root` format
  instead of plain `root`.
- `spring.jpa.hibernate.ddl-auto` changed from `update` to `none`, since the
  schema was migrated as-is and we don't want Hibernate silently altering
  tables or clashing with the manually-added foreign keys on startup.

> ⚠️ **The committed `application.properties` does not currently match this.** It
> still has `spring.datasource.url` pointing at `localhost:3306/shuttle_db` and
> `spring.jpa.hibernate.ddl-auto=update`. The consequence is not theoretical: with
> `update` active, Hibernate has already downgraded `trip.status` from
> `ENUM(...)` to `varchar(255)` — see `doc/03_restore_trip_status_enum.sql` — and it
> re-attempts two ALTERs on `registration_number` that fail against the
> `fk_trip_vehicle` foreign key on every start. Setting `ddl-auto=none` as
> documented here resolves both.

---

## Schema Migrations Applied After the Initial Migration

The schema is maintained by numbered SQL files run by hand, not by an automated
migration tool. Run them in order against a copy first.

| File | Adds | Phase |
|---|---|---|
| `01_cleanup_and_simulation_schema.sql` | Coordinate/status cleanup; `trip` tracking columns (`current_lat`, `current_lng`, `current_leg_index`, `current_point_index`, `dwell_until`); `trip_leg_route`; `trip_location_history` | 0 |
| `02_trip_stop_status.sql` | `trip_stop.status ENUM('PENDING','ARRIVED') NOT NULL DEFAULT 'PENDING'`, plus a backfill marking stops on COMPLETED trips as ARRIVED | 4 |
| `03_restore_trip_status_enum.sql` | Repair only — restores `trip.status` to its ENUM after `ddl-auto=update` downgraded it | 4 |

### `trip_stop` (current)

```
id, trip_id, latitude, longitude, stop_name, stop_order, student_id, status
```

`status` is written by the simulation engine: `PENDING` until the vehicle reaches
the stop, then `ARRIVED`. Reset to `PENDING` for every stop when a trip is
restarted.

---

## Connecting from Other Stacks

The same TiDB instance can be reached from any language/framework — you just
need five things: **host, port (4000), username, password, database name**,
plus **SSL enabled**. TiDB Cloud rejects any connection that isn't
encrypted, regardless of what's connecting.

### ASP.NET (C#)

**Connection string** (`appsettings.json`):
```json
{
  "ConnectionStrings": {
    "ShuttleDb": "Server=gateway01.eu-central-1.prod.aws.tidbcloud.com;Port=4000;Database=shuttle_db;Uid=4Bos9C2srKVNsaB.root;Pwd=2WBpDoD567FHjqYB;SslMode=VerifyFull;"
  }
}
```

Requires the **Pomelo.EntityFrameworkCore.MySql** NuGet package (not
`System.Data.SqlClient`, which is for SQL Server):
```bash
dotnet add package Pomelo.EntityFrameworkCore.MySql
```

In `Program.cs`:
```csharp
var connectionString = builder.Configuration.GetConnectionString("ShuttleDb");
builder.Services.AddDbContext<ShuttleDbContext>(options =>
    options.UseMySql(connectionString, ServerVersion.AutoDetect(connectionString)));
```

`SslMode=VerifyFull` is the EF Core equivalent of Java's
`sslMode=VERIFY_IDENTITY`.

### Node.js / JavaScript

Using **mysql2**:
```bash
npm install mysql2
```

```javascript
const mysql = require('mysql2/promise');

const connection = await mysql.createConnection({
  host: 'gateway01.eu-central-1.prod.aws.tidbcloud.com',
  port: 4000,
  user: '4Bos9C2srKVNsaB.root',
  password: '2WBpDoD567FHjqYB',
  database: 'shuttle_db',
  ssl: {
    minVersion: 'TLSv1.2',
    rejectUnauthorized: true
  }
});

const [rows] = await connection.execute(
  'SELECT * FROM student WHERE email = ?',
  ['test@mandela.ac.za']
);
console.log(rows);
```

If using **Prisma** instead, the connection URL format is:
```
DATABASE_URL="mysql://4Bos9C2srKVNsaB.root:2WBpDoD567FHjqYB@gateway01.eu-central-1.prod.aws.tidbcloud.com:4000/shuttle_db?sslaccept=strict"
```

---

## ⚠️ Security Notes — Read Before Committing to Git

This document (and the config above) contains **live credentials**:
the TiDB database password, and two third-party API keys (OpenRouteService,
Geoapify). Treat this file the same way you'd treat a password vault entry:

- **Do not commit this file, or `application.properties` with real values
  filled in, to a public GitHub repo.** Add `application.properties` to
  `.gitignore` and instead commit an `application.properties.example` with
  placeholder values.
- **Reset the TiDB password** before relying on this long-term — it's been
  shared in chat/screenshots multiple times during setup, so treat it as
  compromised. Go to TiDB Cloud → Connect → **Reset Password**, then update
  this doc and your local config with the new one.
- For the **Render deployment**, don't hardcode these values in
  `application.properties` at all — set them as environment variables in the
  Render dashboard instead (`DB_HOST`, `DB_USER`, `DB_PASSWORD`, etc.), and
  reference them via `${DB_PASSWORD}` syntax in the properties file. This
  keeps secrets out of your codebase entirely.

---

## Next Step

Deploy the Dockerized Spring Boot backend to Render, using the TiDB
connection details above as environment variables rather than hardcoded
values, so the live backend on Render talks to the same `shuttle_db` you
just verified locally.
