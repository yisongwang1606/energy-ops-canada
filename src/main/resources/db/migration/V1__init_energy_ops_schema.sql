create table sites (
    site_id varchar(40) primary key,
    name varchar(255) not null,
    city varchar(255) not null,
    province varchar(2) not null,
    postal_code varchar(7) not null,
    timezone varchar(255) not null,
    status varchar(30) not null,
    notes varchar(1000)
);

create table app_users (
    id bigserial primary key,
    username varchar(80) not null unique,
    full_name varchar(255) not null,
    password_hash varchar(255) not null,
    role varchar(40) not null,
    home_province varchar(2) not null,
    active boolean not null
);

create table assets (
    asset_id varchar(40) primary key,
    name varchar(255) not null,
    asset_type varchar(40) not null,
    site_id varchar(40) not null references sites(site_id),
    status varchar(30) not null,
    baseline_temperature_c double precision not null,
    temperature_safety_margin_c double precision not null,
    vibration_threshold_mm_s double precision not null,
    pressure_low_kpa double precision not null,
    pressure_high_kpa double precision not null,
    current_upper_amp double precision not null,
    minimum_flow_rate_m3_h double precision not null,
    latest_health_score double precision not null,
    latest_failure_risk double precision not null,
    latest_reading_at timestamp(6)
);

create table sensor_readings (
    reading_id varchar(40) primary key,
    timestamp timestamp(6) not null,
    site_id varchar(40) not null references sites(site_id),
    asset_id varchar(40) not null references assets(asset_id),
    temperature_c double precision not null,
    pressure_kpa double precision not null,
    vibration_mm_s double precision not null,
    current_a double precision not null,
    flow_rate_m3_h double precision not null,
    health_score double precision not null,
    anomaly_score double precision not null,
    predicted_failure_risk double precision not null,
    operating_status varchar(255) not null,
    alert_flag boolean not null,
    alert_type varchar(80),
    maintenance_priority varchar(20) not null
);

create table alerts (
    id bigserial primary key,
    alert_code varchar(30) not null unique,
    alert_type varchar(80) not null,
    severity varchar(20) not null,
    status varchar(20) not null,
    priority varchar(20) not null,
    site_id varchar(40) not null references sites(site_id),
    asset_id varchar(40) not null references assets(asset_id),
    reading_id varchar(40) references sensor_readings(reading_id),
    created_at timestamp(6) not null,
    last_observed_at timestamp(6) not null,
    acknowledged_at timestamp(6),
    resolved_at timestamp(6),
    assigned_to varchar(80),
    message varchar(160) not null,
    recommended_action varchar(200),
    dedupe_key varchar(160) not null,
    notes varchar(1000)
);

create table work_orders (
    id bigserial primary key,
    work_order_code varchar(30) not null unique,
    title varchar(140) not null,
    description varchar(1200),
    priority varchar(20) not null,
    status varchar(20) not null,
    site_id varchar(40) not null references sites(site_id),
    asset_id varchar(40) not null references assets(asset_id),
    alert_id bigint references alerts(id),
    created_at timestamp(6) not null,
    updated_at timestamp(6) not null,
    due_date date,
    completed_at timestamp(6),
    assigned_to varchar(80),
    created_by varchar(80),
    completion_notes varchar(1000),
    downtime_minutes integer
);

create table maintenance_records (
    id bigserial primary key,
    work_order_id bigint not null references work_orders(id),
    root_cause varchar(200) not null,
    action_taken varchar(400) not null,
    downtime_minutes integer,
    parts_replaced varchar(200),
    notes varchar(1000),
    created_at timestamp(6) not null,
    created_by varchar(80)
);

create table audit_logs (
    id bigserial primary key,
    actor varchar(80) not null,
    action varchar(80) not null,
    entity_type varchar(60) not null,
    entity_id varchar(80) not null,
    details varchar(1200),
    created_at timestamp(6) not null
);

create index idx_assets_site_id on assets (site_id);
create index idx_sensor_readings_asset_timestamp on sensor_readings (asset_id, timestamp desc);
create index idx_sensor_readings_site_timestamp on sensor_readings (site_id, timestamp desc);
create index idx_alerts_status_created_at on alerts (status, created_at desc);
create index idx_alerts_asset_status on alerts (asset_id, status);
create index idx_alerts_dedupe_status on alerts (dedupe_key, status);
create index idx_work_orders_status_created_at on work_orders (status, created_at desc);
create index idx_work_orders_asset_id on work_orders (asset_id);
create index idx_maintenance_records_work_order_id on maintenance_records (work_order_id);
create index idx_audit_logs_created_at on audit_logs (created_at desc);
