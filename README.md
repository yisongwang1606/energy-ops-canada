# Energy Ops Canada

`Energy Ops Canada` is an English-language energy operations monitoring and predictive maintenance MVP built from the provided requirements document and the 500-row sample telemetry CSV.

It is tailored for a Canadian demo context:

- English UI and documentation
- `en-CA` formatting and Alberta demo sites
- Metric units throughout (`deg C`, `kPa`, `mm/s`, `m3/h`)
- Canadian province and postal code validation for site data
- Mountain Time (`America/Edmonton`) defaults for dashboards and API serialization

## What is included

- Spring Boot backend with JWT authentication
- H2 in-memory database for fast demo startup
- Seeded Canadian sites, assets, users, alerts, work orders, and maintenance history
- CSV import of the provided `energy_ops_sensor_readings_500.csv`
- Dashboard, alert centre, work order tracking, maintenance records, and audit log APIs
- Static English frontend served by the same Spring Boot app
- Optional FastAPI-based Python scoring service for `/ml/predict/*`
- Optional Docker and Docker Compose files for containerized startup

## Demo users

Use any of the seeded accounts below:

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin.ca` | `admin123` |
| Operations Engineer | `ops.lead` | `ops123` |
| Technician | `morgan.tech` | `tech123` |

## Local run

### 1. Run the Java platform

```powershell
.\mvnw.cmd spring-boot:run
```

Then open:

- App UI: `http://localhost:8080`
- H2 console: `http://localhost:8080/h2-console`

H2 settings:

- JDBC URL: `jdbc:h2:mem:energyops`
- Username: `sa`
- Password: empty

### 2. Optional: run the Python ML service

In a second terminal:

```powershell
cd ml-service
py -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8001
```

If you want the Java app to call the Python service, set:

```powershell
$env:ENERGY_ML_ENABLED="true"
$env:ENERGY_ML_BASE_URL="http://localhost:8001"
.\mvnw.cmd spring-boot:run
```

## Docker run

Container support is included but optional.

```powershell
docker compose up --build
```

This starts:

- `energy-ops-app` on `http://localhost:8080`
- `energy-ops-ml` on `http://localhost:8001`

## Key features mapped to the requirement document

- Authentication and role isolation: admin, operations engineer, technician
- Site and asset management: site and asset APIs with Canadian data validation
- Sensor data ingestion: manual API ingest and startup CSV import
- Health and failure scoring: rule-based engine with optional Python sidecar
- Alert management: generation, acknowledgement, assignment, resolution, deduplication
- Work order management: create from alert or asset, track status and due dates
- Maintenance records: root cause, actions, downtime, parts, notes
- Dashboard analytics: 24-hour trend, site risk, asset ranking, alert/work-order summaries
- Export and audit: CSV export for alerts and audit log tracking

## API highlights

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/dashboard/overview`
- `GET /api/sites`
- `GET /api/assets`
- `GET /api/assets/{assetId}`
- `POST /api/sensor-readings`
- `GET /api/alerts`
- `PATCH /api/alerts/{alertId}`
- `GET /api/alerts/export`
- `GET /api/work-orders`
- `POST /api/work-orders`
- `PATCH /api/work-orders/{workOrderId}`
- `GET /api/maintenance-records`
- `POST /api/maintenance-records`
- `GET /api/audit-logs`

## Project structure

```text
.
├── src/main/java/ca/yisong/energyops
│   ├── api
│   ├── controller
│   ├── model
│   ├── repository
│   ├── security
│   ├── service
│   └── support
├── src/main/resources
│   ├── application.properties
│   ├── energy_ops_sensor_readings_500.csv
│   └── static
├── ml-service
│   ├── app.py
│   ├── Dockerfile
│   └── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

## Verification

The project has been compiled and tested with:

```powershell
.\mvnw.cmd test
```

## Notes

- The backend seeds demo data on startup into an in-memory H2 database.
- This is an MVP/demo build designed for portfolio, prototype, and interview use.
- The sample CSV is simulated data only and should not be treated as production telemetry.
