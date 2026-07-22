# Smart Estate

A web-based **building-management system** for Hong Kong residential estates, built as a CSIT321
capstone prototype. It ships with a fictional demo estate, **Evergreen Bay Estate** (six towers),
and demonstrates multi-channel notifications, issue tracking, a colour-coded floor map, emergency
broadcast, fee management, and an AI bylaw assistant. The entire UI is **bilingual (English /
繁體中文)** and switchable at runtime.

External messaging channels (SMS / WhatsApp / Email / Voice) run through a **config-driven AWS
dispatcher**, and fall back to a built-in mock engine when AWS is not configured, so the whole app
runs and demos fully offline out of the box.

---

## Tech stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17 · Spring Boot 3.3.5 (Spring MVC, Spring Data JPA, Spring Security) |
| Frontend | **React 18 + Vite "islands"** on top of Thymeleaf views · bilingual **English / 繁體中文** (runtime switcher) · light/dark, responsive |
| Database | H2 in-memory (seeded on every startup) · **optional Postgres + pgvector** for dense retrieval |
| Auth | Session-based form login · BCrypt · URL-level RBAC (Resident / Management / Admin) |
| Messaging | AWS End User Messaging **SMS**/**Voice**, Amazon **SES** (email), End User Messaging **Social** (WhatsApp); config-driven, with a mock fallback |
| AI | In-house TF-IDF retrieval + k-NN classifier (offline) · **optional dense/hybrid retrieval (pgvector + a local multilingual embedding model)** · optional Anthropic **Claude** (`claude-opus-4-8`) |

---

## Quick start (one command)

**Just the app** (H2 in-memory, offline; no Postgres, no MQTT):
```bash
cd smart-estate
./run.sh
```
Open **http://localhost:8080**. `run.sh` builds the React bundle (only if its sources changed),
pins JDK 17, and launches via the Maven wrapper. First run downloads Maven + npm dependencies.

**Everything** runs the full demo (dense/hybrid AI retrieval + IoT sensors/access) in one command:
```bash
cd smart-estate
./run-all.sh
```
`run-all.sh` also brings up **Postgres + pgvector** (dense/hybrid retrieval), the **embedding
service** (:8000), the **MQTT broker** (:1883) + IoT ingest, and the **device simulator**
(**http://localhost:8090**), then starts the app with those features on. Any component whose
prerequisites are missing (Postgres/pgvector, `mosquitto`, Python) is skipped with a warning and the
app still starts with the rest. **Ctrl+C stops everything.** Opt out of a part with
`RUN_PGVECTOR=0 ./run-all.sh` or `RUN_IOT=0 ./run-all.sh`. Prerequisites for the full stack:
`brew install postgresql@15 pgvector mosquitto` and Python 3 (see the pgvector + IoT sections below).

Alternatively:
```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw spring-boot:run
# or build a runnable jar (the React bundle is committed, so this alone is enough):
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw clean package
java -jar target/smart-estate.jar
```

### Demo accounts
| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@smartestate.hk` | `admin123` |
| Management | `manager@smartestate.hk` | `manager123` |

> **~48 fictional demo residents are seeded by default** (all password `resident123`, e.g.
> `resident@smartestate.hk`), so the floor map, Fees and inboxes are populated from the first page
> load. For a public deployment that must store no resident PII, set `app.seed.residents=false`
> (`--app.seed.residents=false` on the command line); the floor map then starts fully vacant.

H2 console (dev only): **http://localhost:8080/h2-console** (JDBC `jdbc:h2:mem:smartestate`, user `sa`, no password).

---

## Features

- **Multi-channel notifications (FR-1):** per-resident channel preference routing, delivery status
  + read receipts, scheduled sending, batch-by-block.
- **Management dashboard (FR-2):** resident database (search / filter / CRUD), notification history
  & delivery analytics, announcement compose **+ edit**, issue tracking, per-unit fee management.
- **Resident dashboard (FR-3):** inbox, communication preferences, issue reporting with photo,
  fee status, maintenance schedule.
- **Building & floor map (FR-4):** register buildings/blocks/floors/units, colour-coded floor map
  with drill-down and per-tower occupancy summary.
- **Issue management (FR-5):** auto tracking number, status workflow, priority, resident/management
  comment thread, resident withdraw.
- **HK templates (FR-6):** Observatory alerts, bilingual BMO meeting notices, automated fee
  reminders with overdue escalation, service-interruption alerts.
- **Emergency (FR-7):** declare, forced multi-channel broadcast, voice escalation, audit log, close.
- **AI assistant (FR-8):** bilingual bylaw/FAQ chatbot with **switchable retrieval** (TF-IDF, or
  optional dense / hybrid semantic + cross-lingual retrieval on pgvector), optional Claude answer
  generation, document ingestion + re-index, issue auto-categorisation (k-NN), evaluation page.
- **Bilingual UI (§5.1):** every page in English and 繁體中文 with a runtime switcher (seeds from the
  account's language); server-generated notices, flash messages and notification titles are bilingual
  too. The companion iOS push app (`../ios-app`) is likewise localised.
- **IoT: sensors + access control** *(opt-in)*: an estate device fleet publishes over **MQTT**; the
  backend ingests and pushes **live** updates to the dashboards over SSE. Sensors are typed:
  smoke/heat (fire, escalate to an emergency in one click), **fire doors** (staircase
  doors that must stay shut, so a safety *warning*, not an emergency), general/service **doors**, and
  water-leak, located per-unit and per-floor. Octopus **access cards** open the shared doors
  (**building entrance and lift**), and the management **Access control** page is the live access log
  (searchable + sortable) plus a **maker-checker** approval queue for residents' card registrations
  (movement data is management-only, card numbers masked). The Sensor and Access pages support
  search + column sort. A standalone simulator (`iot-simulator/`) with a hand-control panel stands
  in for the physical devices.

Enabled channels are configurable; the app currently ships **SMS-only** (`app.channels.enabled=SMS`),
and the resident preferences UI shows the other channels as disabled.

---

## Configuration

All behaviour is driven by `src/main/resources/application.properties` and can be overridden per
deployment via environment variables (Spring relaxed binding). **Secrets and account-specific
identifiers are never committed**; supply them at runtime.

### Enable AWS messaging (SMS / Voice / SES / WhatsApp)
The repo default is **off** (mock engine). To route real messages, provide credentials + identities
via env (e.g. in a systemd `EnvironmentFile`):

```bash
APP_AWS_MESSAGING_ENABLED=true
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-southeast-1
# SMS origination (phone-number ARN, pool id, or sender id):
APP_AWS_MESSAGING_SMS_ORIGINATIONIDENTITY=arn:aws:sms-voice:ap-southeast-1:<acct>:sender-id/<id>/HK
# (SES: APP_AWS_MESSAGING_EMAIL_FROM=... ; WhatsApp: APP_AWS_MESSAGING_WHATSAPP_ORIGINATIONPHONENUMBERID=...)
```
The IAM principal needs `sms-voice:SendTextMessage`, `sms-voice:SendVoiceMessage`, `ses:SendEmail`,
`social-messaging:SendWhatsAppMessage`. The channel-to-AWS-service mapping lives in
`config/AwsMessagingProperties.java`.

### Enable Claude (AI answer generation)
Claude is used only when a key is present; otherwise the local TF-IDF engine answers.
```bash
ANTHROPIC_API_KEY=sk-ant-...
```

### Optional: dense / hybrid retrieval (pgvector)

By default the assistant retrieves with **lexical** TF-IDF (offline, no dependencies). An opt-in
`pgvector` profile adds **dense** (semantic) and **hybrid** retrieval: a query matches passages by
meaning, and (using a multilingual embedding model) a 中文 query can retrieve an English passage and
vice-versa. The default profile is unchanged, so the app still boots with no external services.

```bash
# 1. Postgres + pgvector (once)
brew install postgresql@15 && brew services start postgresql@15
createdb smartestate
psql -d smartestate -c "CREATE EXTENSION vector;"     # build pgvector for pg15 if the bottle targets a different major

# 2. Local embedding service (no API key; runs a multilingual model on this machine)
cd embedding-service && ./run.sh                       # serves :8000, first run downloads ~470 MB

# 3. Run the app on Postgres with hybrid retrieval
./mvnw spring-boot:run -Dspring-boot.run.profiles=pgvector
```

Retrieval strategy is set in `application-pgvector.properties` (`app.ai.retrieval.mode` =
`lexical` | `dense` | `hybrid`); switch it to A/B the three retrievers on the same corpus. If the
embedding service is down, retrieval automatically falls back to TF-IDF. Components:
`ai/EmbeddingClient`, `ai/PgVectorStore`, and the strategy dispatch in `ai/RagService`;
the vectoriser lives in `embedding-service/`.

### Optional: IoT sensors + access control (MQTT + live dashboard)

An estate device fleet publishes over MQTT; the backend ingests and streams live updates to the
dashboard (SSE). **Off by default** (`app.iot.mqtt.enabled=false`) so the app boots with no broker.

```bash
# 1. MQTT broker (once)
brew install mosquitto
mosquitto -c iot-simulator/mosquitto.conf      # listens on localhost:1883

# 2. Run the app with IoT ingest enabled (works on H2 or the pgvector profile)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--app.iot.mqtt.enabled=true

# 3. Device simulator: an interactive control panel for a demo
cd iot-simulator && ./run.sh                    # http://localhost:8090
```

Then, signed in as management/admin: **Sensors** (`/mgmt/iot`) shows live sensor status; trip a
smoke sensor in the simulator and it appears instantly, with a one-click **Declare emergency** (fire
doors raise a safety warning only). **Access control** (`/mgmt/access`) is the card-registration
approval queue + the live entrance/lift access log; both the sensor and access tables are searchable
and sortable. Residents manage their own card under **Access card** (`/resident/access`). MQTT
topics: `estate/sensor/{code}` and `estate/access/{accessPointCode}`. If the broker is down, ingest
is off and the rest of the app is unaffected. Components live in `iot/` (MQTT ingest, SSE bus,
sensor + access services) with the device fleet simulated in `iot-simulator/`.

---

## Deploying to AWS Lightsail (summary)

1. `./mvnw clean package` builds `target/smart-estate.jar` (contains the React bundle).
2. `scp` the jar to the instance; install `openjdk-17-jre-headless`.
3. Put secrets in a root-only `EnvironmentFile` (`/etc/smartestate.env`, `chmod 600`).
4. Run as a `systemd` service (`ExecStart=/usr/bin/java -jar /opt/smartestate/smart-estate.jar`,
   `EnvironmentFile=/etc/smartestate.env`), then `systemctl enable --now smartestate`.
5. Put **nginx + HTTPS** in front and keep port 8080 closed.

### Security notes for real deployments
- Change the default `admin` / `manager` passwords.
- Set `spring.h2.console.enabled=false`.
- Terminate TLS at a reverse proxy (no plain HTTP).
- Set `app.seed.residents=false` (no demo-resident PII) in production.

---

## Frontend (React islands)

The UI is a Vite + React app in [`frontend/`](frontend/), rendered as per-page *islands*: each Spring
controller returns a thin Thymeleaf view that injects its data as JSON (`window.__APP__` /
`window.__PAGE__`) and mounts a React component. Backend routes, auth, CSRF and file uploads are
unchanged; forms still POST to the same endpoints.

```bash
cd frontend
npm install          # once
npm run build        # outputs ../src/main/resources/static/app/{smart-estate.js,.css}
npm run dev          # optional: Vite dev server with mock data (?page=<name>)
```
The build output is committed under `src/main/resources/static/app/`, so the app runs without Node.

---

## Project structure
```
smart-estate/
├── src/main/java/com/smartestate/
│   ├── web/            # controllers (Home/Auth, Admin, Emergency, Management, Resident, Ai, File)
│   ├── service/        # dispatch, recipients, emergency, fees, issues, floor map, audit, scheduler
│   ├── messaging/      # config-driven AWS senders (SMS/Voice/SES/WhatsApp) + mock fallback
│   ├── iot/            # MQTT ingest, SSE event bus, sensor + access-control services
│   ├── ai/             # RAG (TF-IDF + optional pgvector dense/hybrid), EmbeddingClient,
│   │                   #   VectorStore/PgVectorStore, k-NN classifier, evaluation, Claude client
│   ├── model/ · repo/  # JPA entities + repositories
│   ├── config/         # security, AWS + channel policy config
│   └── seed/           # in-memory demo data seeder
├── src/main/resources/
│   ├── templates/      # Thymeleaf island shells (per page)
│   ├── static/app/     # committed React bundle
│   ├── application.properties            # default (H2, offline, TF-IDF)
│   └── application-pgvector.properties   # opt-in Postgres + dense/hybrid retrieval profile
├── frontend/           # React 18 + Vite source (bilingual i18n in src/lib/i18n.js, strings.js)
├── embedding-service/  # local multilingual embedding sidecar (Python/FastAPI) for dense retrieval
├── iot-simulator/      # standalone device-fleet simulator + mosquitto.conf (MQTT)
├── run.sh              # one command: just the app (H2, offline)
├── run-all.sh          # one command: full stack (pgvector + IoT + simulator)
├── pom.xml · mvnw
```

## Notes
- H2 is in-memory: data resets on restart. CRUD edits persist within a single run. (Under the
  `pgvector` profile the app runs on Postgres, so data and the embedding index persist.)
- External SMS / WhatsApp / Email / Voice are **mocked** unless AWS is configured; each "send" is
  otherwise recorded as a delivery-log row with a simulated status.
- Retrieval defaults to offline TF-IDF; dense/hybrid is opt-in and falls back to TF-IDF automatically
  if the embedding service is unavailable, so the assistant always answers.
