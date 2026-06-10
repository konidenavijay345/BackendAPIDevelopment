# Product CRUD REST API

Production-style Java API for creating, reading, updating, and deleting products in local MySQL. It can be called by websites, mobile apps, Postman, or any HTTP client.

## Stack

- Java 21 target (compatible with the installed JDK 24)
- Spring Boot 3.5, Web, Validation, Data JPA, and Actuator
- MySQL 8 with HikariCP connection pooling
- Flyway schema migrations
- Maven, JUnit, Mockito, and MockMvc

## File Guide

```text
src/main/java/com/example/backendapi/
|-- BackendApiApplication.java       application entry point
|-- config/WebConfig.java            frontend CORS configuration
|-- product/
|   |-- Product.java                 database entity
|   |-- ProductRequest.java          validated create/update input
|   |-- ProductResponse.java         public response contract
|   |-- ProductRepository.java       JPA data access
|   |-- ProductService.java          transactions and business rules
|   `-- ProductController.java       REST routes and status codes
`-- shared/
    |-- ApiError.java                 standard JSON error contract
    |-- ConflictException.java        duplicate-data error
    |-- ResourceNotFoundException.java
    `-- GlobalExceptionHandler.java   centralized error handling

src/main/resources/application.yml   database, pool, server, CORS settings
src/main/resources/db/migration/     versioned database schema
src/test/                             API tests
postman/                              importable Postman collection
.env.example                          environment variable reference
pom.xml                               dependencies and build
```

## Request Flow

1. `ProductController` receives JSON at `/api/v1/products`.
2. `ProductRequest` validation rejects bad input before business logic runs.
3. `ProductService` normalizes data, enforces unique SKUs, and owns transactions.
4. `ProductRepository` uses JPA to access MySQL.
5. `ProductResponse` keeps the API contract separate from the database entity.
6. `GlobalExceptionHandler` returns consistent, safe JSON errors.

Flyway creates the table on startup. Add future schema changes as new files such as `V2__add_product_category.sql`. Do not modify a migration after it has run in a shared environment.

## Local Setup

Prerequisites: MySQL 8 on `localhost:3306`, JDK 21+, and Maven 3.9+ or the included Maven Wrapper. The local `MYSQL80` service was running when this project was built.

Defaults:

```text
database: crud_api
username: root
password: empty
```

The URL uses `createDatabaseIfNotExist=true`; the MySQL user must have permission to create the database. Set credentials in PowerShell when your root account has a password:

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
.\mvnw.cmd spring-boot:run
```

Optional settings:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/crud_api?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SERVER_PORT="8080"
$env:ALLOWED_ORIGINS="http://localhost:3000,http://localhost:5173"
```

Do not commit real passwords. `.env.example` documents values, but Spring Boot does not automatically load `.env`; use the shell, IDE run configuration, deployment platform, or a secrets manager.

Health check after startup:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

## Endpoints

| Operation | Method | URL | Success |
|---|---|---|---|
| Create | `POST` | `/api/v1/products` | `201 Created` |
| List | `GET` | `/api/v1/products?page=0&size=20&sort=id,asc` | `200 OK` |
| Read | `GET` | `/api/v1/products/{id}` | `200 OK` |
| Update | `PUT` | `/api/v1/products/{id}` | `200 OK` |
| Delete | `DELETE` | `/api/v1/products/{id}` | `204 No Content` |
| Health | `GET` | `/actuator/health` | `200 OK` |

List results are paginated. `page` starts at zero, `size` controls page length, and `sort=price,desc` selects sorting.

Create/update JSON:

```json
{
  "name": "Mechanical Keyboard",
  "sku": "KEY-001",
  "description": "Hot-swappable mechanical keyboard",
  "price": 89.99,
  "quantity": 25
}
```

Validation rules:

- `name`: required, maximum 150 characters
- `sku`: required and unique, maximum 64 characters, stored uppercase
- `description`: optional, maximum 1000 characters
- `price`: required, zero or greater, at most two decimal places
- `quantity`: required, zero or greater

Example validation error:

```json
{
  "timestamp": "2026-06-10T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/products",
  "validationErrors": { "name": "must not be blank" }
}
```

The API uses `400` for invalid input, `404` for missing records, `409` for duplicate data, and `500` for unexpected errors. Stack traces are logged, not exposed to clients.

## Postman

1. In Postman, select **Import**.
2. Import `postman/Product-CRUD-API.postman_collection.json`.
3. Start the API and run **Create product** first.
4. Its test script stores the returned ID as `productId`; read, update, and delete requests then use that ID.

The collection's `baseUrl` defaults to `http://localhost:8080`.

## Website Integration

CORS permits `http://localhost:3000` and `http://localhost:5173` by default. Set `ALLOWED_ORIGINS` to comma-separated production frontend URLs when deploying.

```javascript
const response = await fetch("http://localhost:8080/api/v1/products", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    name: "Mechanical Keyboard",
    sku: "KEY-001",
    description: "Hot-swappable mechanical keyboard",
    price: 89.99,
    quantity: 25
  })
});

if (!response.ok) throw await response.json();
const product = await response.json();
```

Add authentication and authorization before exposing write endpoints on the internet. CORS is not authentication.

## Build and Test

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
java -jar target/backend-api-development-1.0.0.jar
```
