FROM timescale/timescaledb-ha:pg14-latest
COPY init.sql /docker-entrypoint-initdb.d/