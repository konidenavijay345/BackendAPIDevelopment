# Deployment

This Spring Boot API is container-ready through the included `Dockerfile`.

## Required Environment Variables

Set these in the hosting provider:

```text
DB_URL=jdbc:mysql://<host>:<port>/<database>?useSSL=true&serverTimezone=UTC
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
JWT_SECRET=<random-secret-at-least-32-characters>
ALLOWED_ORIGIN_PATTERNS=https://your-frontend.example.com
```

Most hosting providers inject `PORT`; the app now uses it automatically. If a provider does
not inject `PORT`, it listens on `SERVER_PORT` or `8080`.

## Health Check

Use:

```text
/actuator/health
```

## Railway

1. Create a new project from the GitHub repository.
2. Add a MySQL service.
3. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `ALLOWED_ORIGIN_PATTERNS` on the web service.
4. Deploy using the Dockerfile builder.

## Render or Other Docker Hosts

Create a Docker web service from this repository and set the same environment variables.
Use a hosted MySQL database, because the API requires MySQL and Flyway migrations run at startup.
