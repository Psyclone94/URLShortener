# url-shortener

Java / Spring Boot URL shortener, built locally. See the design notes for architecture decisions.

## Step 0: setup

### 1. Start infrastructure

```bash
docker compose up -d
docker compose ps        # all three services should be healthy or running
```

Services: Postgres on `5432`, Redis on `6379`, Kafka on `9092`.

### 2. Build and run the app

```bash
mvn spring-boot:run
```

### 3. Verify

```bash
curl -s http://localhost:8080/actuator/health
```

Expected: `{"status":"UP", ...}`

Also confirm the infrastructure is reachable:

```bash
docker exec -it urlshort-redis redis-cli ping                 # PONG
docker exec -it urlshort-postgres psql -U urlshort -d urlshort -c "select 1;"
```

### 4. Run the tests

```bash
mvn test
```

## Step 0 done when

- `docker compose ps` shows Postgres, Redis, and Kafka running
- `/actuator/health` returns `UP`
- `mvn test` passes
