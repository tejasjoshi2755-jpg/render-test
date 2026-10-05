# Neon Cache Demo - Spring Boot

Java 17 + Spring Boot + PostgreSQL (Neon).

## Flow

### POST /api/cache/get

Request:

```json
{
  "value": "051026205730"
}
```

The service does:

1. Save the request value to PostgreSQL.
2. Check static L1 `ConcurrentHashMap`.
3. If key `12345` exists, return its value.
4. If key `12345` does not exist, call `send()`.
5. `send()` clears L1 and stores the current timestamp in `ddMMyyHHmmss` format under key `12345`.
6. Return the newly stored value.

### POST /api/cache/send

No request body is required.

It:

1. Clears L1.
2. Generates current time using `ddMMyyHHmmss`.
3. Stores it as:
   `12345 -> currentTime`
4. Returns the value.

## Environment variables

Set:

```text
DATABASE_URL=jdbc:postgresql://YOUR_NEON_HOST/neondb?sslmode=require&channel_binding=require
DB_USERNAME=neondb_owner
DB_PASSWORD=YOUR_PASSWORD
```

Do not commit passwords to Git.

## Run

```bash
mvn spring-boot:run
```

## Test

### First request

```bash
curl -X POST http://localhost:8080/api/cache/get ^
  -H "Content-Type: application/json" ^
  -d "{"value":"051026205730"}"
```

Expected:

```json
{
  "key": "12345",
  "value": "051026205731",
  "source": "SEND"
}
```

### Second request

```bash
curl -X POST http://localhost:8080/api/cache/get ^
  -H "Content-Type: application/json" ^
  -d "{"value":"051026205800"}"
```

The request is saved to DB, but the response comes from L1:

```json
{
  "key": "12345",
  "value": "051026205731",
  "source": "L1_CACHE"
}
```

### Clear and refresh L1

```bash
curl -X POST http://localhost:8080/api/cache/send
```

Example:

```json
{
  "key": "12345",
  "value": "051026205845",
  "source": "SEND"
}
```
