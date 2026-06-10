# Code and OOP Guide

This guide explains how the project works, why each layer exists, which object-oriented principles it demonstrates, and how those choices benefit companies and API users.

## Complete Request Flow

### Registration and login

1. `AuthController` receives and validates JSON.
2. `AuthService` normalizes the email and coordinates the use case.
3. `PasswordEncoder` hashes new passwords with BCrypt or verifies login passwords.
4. `UserRepository` reads or writes the user in MySQL.
5. `JwtService` signs a token that expires after 10 minutes.
6. `AuthResponse` exposes the token and safe user fields, never the password hash.

### Authenticated product request

1. `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`.
2. `JwtService` verifies the token signature and expiry.
3. `UserService` loads the user represented by the token subject.
4. Spring Security stores that identity as a `Principal` for this request.
5. `ProductController` passes the trusted principal email to `ProductService`.
6. `ProductService` applies validation, normalization, transactions, and ownership rules.
7. `ProductRepository` includes `ownerId` in queries so another user's data cannot match.
8. `ProductResponse` returns only the public API fields.

## OOP Concepts Used

### Encapsulation

`AppUser` and `Product` keep fields private. State changes occur through constructors and `Product.update()` instead of arbitrary public setters. This protects invariants such as immutable ownership and keeps changes easy to audit.

Records such as `ProductRequest`, `ProductResponse`, and `AuthResponse` also encapsulate immutable data contracts. Their values cannot be modified after creation.

### Abstraction

`UserRepository` and `ProductRepository` hide SQL and database connection details behind interfaces. Business services express what data they need without knowing how JDBC statements are built.

`PasswordEncoder`, `AuthenticationManager`, `UserDetailsService`, and `WebMvcConfigurer` are framework abstractions. The application depends on their contracts rather than concrete implementation details.

### Inheritance

`JwtAuthenticationFilter` extends `OncePerRequestFilter`, reusing Spring's filter lifecycle while overriding the method that performs JWT authentication.

`ConflictException` and `ResourceNotFoundException` extend `RuntimeException`, allowing business failures to propagate to one centralized exception handler and automatically roll back transactions.

### Polymorphism

`AppUser` implements `UserDetails`, so Spring Security can operate on the application user through its standard security interface. `UserService` implements `UserDetailsService`, allowing Spring to call the application's lookup logic without depending on the concrete service class.

### Composition and Dependency Injection

Controllers contain services, services contain repositories, and security components contain the collaborators they require. Spring supplies these objects through constructors.

Composition keeps each class focused. Constructor injection makes dependencies visible, prevents partially initialized objects, and allows tests to replace collaborators with mocks.

## Design Principles

### Single Responsibility

- Controllers handle HTTP translation.
- Services handle use cases and business rules.
- Repositories handle persistence.
- DTOs define API input and output.
- Entities represent persisted domain state.
- Security classes authenticate requests.
- Exception advice translates failures.

Changing one concern therefore has less risk of breaking unrelated behavior.

### Dependency Inversion

Services depend on repository and security interfaces. A company can replace MySQL persistence, BCrypt configuration, or authentication implementations with limited changes to business code.

### Defense in Depth

JWT authentication establishes who the user is. Product repository queries also require that user's ID. Even if a product ID is guessed, the query cannot return another user's record.

Validation exists both in Java annotations and database constraints. The API provides fast feedback, while MySQL protects data when writes arrive concurrently or from another integration.

## Why Companies Use This Structure

- **Maintainability:** Teams can change one layer without rewriting the whole application.
- **Security:** Password hashing, short-lived JWTs, owner-scoped queries, and safe errors reduce common risks.
- **Scalability:** Stateless JWT requests can be handled by multiple server instances without shared sessions.
- **Testability:** Constructor injection and interfaces allow focused automated tests.
- **Integration:** Stable JSON DTOs work with websites, mobile apps, Postman, and other services.
- **Database control:** Flyway creates an auditable, repeatable history of schema changes.
- **Operational clarity:** Actuator health checks allow deployment platforms to monitor the service.

## User Benefits

- Users receive clear validation messages instead of unexplained failures.
- Their products are isolated from other accounts.
- Passwords are never stored in readable form.
- Stolen JWT usefulness is limited by the 10-minute expiry.
- Pagination keeps product lists responsive as data grows.
- Consistent response formats make frontend behavior predictable.
- Audit timestamps show when records were created and changed.

## Important Annotations

- `@RestController`: exposes Java methods as JSON HTTP endpoints.
- `@Service`: marks business logic managed by Spring.
- `@Entity`: maps a Java object to a database table.
- `@Transactional`: defines an atomic database unit of work.
- `@Valid`: activates request validation annotations.
- `@Configuration` and `@Bean`: define application infrastructure.
- `@RestControllerAdvice`: centralizes HTTP exception handling.
- `@PrePersist` and `@PreUpdate`: run entity lifecycle behavior automatically.

## Where to Start Reading

Read the project in this order:

1. `BackendApiApplication`
2. `SecurityConfig`
3. `AuthController` and `AuthService`
4. `JwtAuthenticationFilter` and `JwtService`
5. `ProductController` and `ProductService`
6. `ProductRepository` and `Product`
7. `GlobalExceptionHandler`

This follows the same direction as an incoming request and makes the dependencies easier to understand.
