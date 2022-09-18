CREATE DATABASE eventstore;
\c eventstore;

CREATE TYPE event_type AS ENUM ('click', 'impression');

CREATE TABLE event_history
(
    id         UUID       NOT NULL DEFAULT gen_random_uuid(),
    event_time TIMESTAMP  NOT NULL,
    username   TEXT       NOT NULL,
    event_type event_type NOT NULL
);

SELECT *
FROM create_hypertable('event_history', 'event_time');

SELECT set_chunk_time_interval('event_history', INTERVAL '1 hour');

SELECT *
FROM show_tablespaces('event_history');

CREATE MATERIALIZED VIEW events_summary_hourly
    WITH (timescaledb.continuous) AS
SELECT count(distinct username)                                   AS unique_users,
       sum(case when event_type = 'click' then 1 else 0 end)      AS total_clicks,
       sum(case when event_type = 'impression' then 1 else 0 end) AS total_impressions,
       time_bucket(INTERVAL '1 hour', event_time)                 AS bucket
FROM event_history
GROUP BY bucket;

SELECT add_continuous_aggregate_policy(
               'events_summary_hourly',
               start_offset => INTERVAL '1 month',
               end_offset => INTERVAL '1 h',
               schedule_interval => INTERVAL '10 s');
