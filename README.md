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