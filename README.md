# User-Owned Product CRUD API

Spring Boot REST API with MySQL, user registration/login, BCrypt password hashing, and JWT-protected product CRUD. Each user can list, read, update, and delete only their own products.

For a detailed explanation of every layer, request flow, OOP concept, company use case, and user benefit, read [CODE_DOCUMENTATION.md](CODE_DOCUMENTATION.md). Production Java classes also contain JavaDoc above their responsibilities and methods.

## Security Behavior

- `POST /api/v1/auth/register` and `POST /api/v1/auth/login` are public.
- Every `/api/v1/products/**` request requires `Authorization: Bearer <token>`.
- JWTs expire exactly 10 minutes (`600` seconds) after issue.
- Sessions are stateless; the server does not store JWTs.
- Passwords are stored as BCrypt hashes, never plaintext.
- Every product query includes the authenticated user's ID.
- Accessing another user's product returns `404`, avoiding record enumeration.
- SKUs are unique per user, so two users may use the same SKU.

## Main Files

```text
config/SecurityConfig.java                 HTTP security and BCrypt
security/JwtService.java                   creates and verifies 10-minute JWTs
security/JwtAuthenticationFilter.java      reads Bearer tokens
auth/AuthController.java                   register and login routes
auth/AuthService.java                      account creation and authentication
user/AppUser.java                          user database entity/UserDetails
user/UserRepository.java                   user persistence
product/ProductController.java             protected CRUD routes
product/ProductService.java                owner-scoped business logic
product/ProductRepository.java             owner-scoped database queries
db/migration/V1__create_products_table.sql initial product schema
db/migration/V2__add_users_and_product_ownership.sql
                                            users and ownership schema
postman/Product-CRUD-API.postman_collection.json
                                            importable API collection
```

## Run From Windows CMD

Open Command Prompt and run:

```bat
cd /d C:\Users\DELL\IdeaProjects\BackendAPIDevelopment
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password
set JWT_SECRET=replace-this-with-a-random-secret-at-least-32-characters
mvnw.cmd spring-boot:run
```

The API starts at `http://localhost:8080`. Keep that CMD window open while using Postman. Stop it with `Ctrl+C`.

To build and run the packaged application instead:

```bat
cd /d C:\Users\DELL\IdeaProjects\BackendAPIDevelopment
set DB_USERNAME=root
set DB_PASSWORD=your_mysql_password
set JWT_SECRET=replace-this-with-a-random-secret-at-least-32-characters
mvnw.cmd clean package
java -jar target\backend-api-development-1.0.0.jar
```

`JWT_SECRET` must be at least 32 characters and should be random in production. Environment variables are intentionally not committed with real credentials.

## Authentication API

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Vijay",
  "email": "vijay@example.com",
  "password": "Password@123"
}
```

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "vijay@example.com",
  "password": "Password@123"
}
```

Both return:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 600,
  "user": {
    "id": 1,
    "name": "Vijay",
    "email": "vijay@example.com"
  }
}
```

When the token expires, login again to receive a new token.

## Product API

All routes below require the JWT header:

```http
Authorization: Bearer eyJ...
```

| Operation | Method | URL | Success |
|---|---|---|---|
| Create | `POST` | `/api/v1/products` | `201` |
| List mine | `GET` | `/api/v1/products?page=0&size=20&sort=id,asc` | `200` |
| Read mine | `GET` | `/api/v1/products/{id}` | `200` |
| Update mine | `PUT` | `/api/v1/products/{id}` | `200` |
| Delete mine | `DELETE` | `/api/v1/products/{id}` | `204` |

Product body:

```json
{
  "name": "Mechanical Keyboard",
  "sku": "KEY-001",
  "description": "Hot-swappable mechanical keyboard",
  "price": 89.99,
  "quantity": 25
}
```

Missing, invalid, or expired JWTs return `401`. Invalid input returns `400`, missing or non-owned products return `404`, and duplicate email/SKU data returns `409`.

## Postman

1. Import `postman/Product-CRUD-API.postman_collection.json`.
2. Run **Authentication > Register user**. It creates a unique email and saves the JWT automatically.
3. Run the product requests. The folder automatically sends `Bearer {{jwtToken}}`.
4. Run **Login user** whenever the 10-minute token expires.

## Website Example

```javascript
const response = await fetch("http://localhost:8080/api/v1/products", {
  headers: { Authorization: `Bearer ${token}` }
});

if (!response.ok) throw await response.json();
const page = await response.json();
```

CORS defaults to `http://localhost:3000` and `http://localhost:5173`. Set `ALLOWED_ORIGINS` to comma-separated production frontend origins.

## Verification

```bat
mvnw.cmd clean test
mvnw.cmd clean package
```
