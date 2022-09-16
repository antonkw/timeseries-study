Docker and SBT are required to run environment and application.

# Env
## Timescale
### Build
``` 
cd src/test/IT
docker compose build --no-cache
```

### Run
```
docker compose up
```

# App

## Run
``` 
sbt run
```

# Endpoints

## Swagger

[http://localhost:8080/docs](http://localhost:8080/docs) is full-featured Swagger.

![image info](./pics/post_screenshot.jpg)

# Description

The project leverages features of [Timescale](https://www.timescale.com/). It is a product built on the top of Postgres.

## Core table.
There is one core table.
``` 
CREATE TABLE event_history
(
    id         UUID       NOT NULL DEFAULT gen_random_uuid(),
    event_time TIMESTAMP  NOT NULL,
    username   TEXT       NOT NULL,
    event_type event_type NOT NULL
);
```

That table is converted to "hypertable" with "1 hour" chunk interval. Which means that the latest hour is being persisted in separate table and also located in the memory.

```sql
SELECT * FROM create_hypertable('event_history', 'event_time');

SELECT set_chunk_time_interval('event_history', INTERVAL '1 hour');
```

## Continuous aggregations
Continuous aggregations allow to main fast access to per-hour aggregates. Writings are still fast. Re-calculations are happening as schedules background jobs.
So, for old date we have a chance to observe slightly outdated report.
But the latest hour is still actual since the delta (new records) is being taken into account.
```sql
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
```

