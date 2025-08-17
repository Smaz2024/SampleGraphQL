# SampleGraphQL

SampleGraphQL is a modern Spring Boot 3 application demonstrating a robust GraphQL API with JWT authentication, Redis caching, and Resilience4j fault tolerance. It is designed for learning and rapid prototyping of secure, resilient backend systems using Java, Spring Boot, and GraphQL.

## Architecture Overview
- **Spring Boot 3**: Main application framework.
- **GraphQL**: API layer, schema in `src/main/resources/schema.graphqls`, entrypoint in `controller/GraphQLController.java`.
- **JWT Security**: Authentication via JWT tokens, implemented in `security/JwtUtil.java` and `security/JwtWebFilter.java`. Tokens are created in login/register mutations and validated for protected endpoints.
- **Role-based Access**: Enforced in service/controller logic. Roles: USER, ADMIN, MODERATOR.
- **Business Logic**: Encapsulated in services (`service/UserService.java`, `service/PostService.java`).
- **Persistence**: JPA repositories (`repository/UserRepository.java`, `repository/PostRepository.java`). Entities in `entity/`.
- **Caching**: Redis, configured in `application-dev.yml`.
- **Resilience**: Circuit breaking, rate limiting, and retries via Resilience4j annotations in service classes.
- **Reactive Programming**: Some services use Reactor (`Mono`, `Flux`) for async operations, especially in security and external service calls.
- **Error Handling**: Custom exceptions in `exception/` mapped to GraphQL errors.
- **H2 Database**: In-memory DB for development, accessible via `/h2-console`.
- **Docker**: App and Redis can be run in Docker. See `Dockerfile` and below for details.

## Features
- GraphQL API for users and posts
- JWT-based authentication and authorization
- Role-based access control (USER, ADMIN, MODERATOR)
- Redis caching for performance
- Resilience4j for circuit breaking, rate limiting, and retries
- H2 in-memory database for development
- Real-time subscriptions (WebSocket)

## Key Files & Directories
- `controller/GraphQLController.java`: Main API entrypoint, routes all queries/mutations.
- `service/UserService.java`, `service/PostService.java`: Core business logic, user/post management, caching, resilience.
- `security/JwtUtil.java`, `security/JwtWebFilter.java`: JWT creation, validation, authentication filter.
- `repository/`: JPA repositories for data access.
- `entity/`: Entity definitions for User, Post, etc.
- `resources/schema.graphqls`: GraphQL schema (types, queries, mutations, subscriptions).
- `resources/application-dev.yml`: Dev config (JWT, Redis, DB, Resilience4j).
- `exception/`: Custom error handling mapped to GraphQL errors.

## Prerequisites
- Java 17+
- Gradle
- Redis (for caching)
- Postman (for API testing)

## Getting Started

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd SampleGraphQL
```

### 2. Start Redis
You must have Redis running locally for caching to work.
- Download and install Redis from [redis.io](https://redis.io/download)
- Start Redis server:
  ```bash
  redis-server
  ```

### 3. Run the Spring Boot Application
You can start the application using Gradle:
```bash
gradlew bootRun
```

### 4. Configuration
- Main configs: `src/main/resources/application.yml` and `application-dev.yml`.
- JWT secret and Redis settings are defined in `application-dev.yml`.
- H2 DB is used for development; access via `/h2-console`.
The app will start on [http://localhost:8080](http://localhost:8080).

### 4. Access GraphiQL UI
For interactive GraphQL queries, visit:
- [http://localhost:8080/graphiql](http://localhost:8080/graphiql)

### 5. Testing with Postman
#### a. Import GraphQL Requests
- Create a new POST request to `http://localhost:8080/graphql`
- Set the request body type to `GraphQL` or `raw` (JSON)
- Example login mutation:
  ```graphql
  mutation {
    login(input: { username: "youruser", password: "yourpassword" }) {
      token
      refreshToken
      user { id username email role }
    }
  }
  ```
- Copy the `token` from the response.

#### b. Use Bearer Token
- For authenticated queries/mutations, add a header:
  - Key: `Authorization`
  - Value: `Bearer <your-token>`

#### c. Example Authenticated Query
```graphql
query {
  me {
    id
    username
    email
    role
  }
}
```

### 6. GraphQL Schema Highlights
- All queries/mutations are defined in `schema.graphqls` and routed via `GraphQLController.java`.
- Core types: `User`, `Post`, `AuthPayload`, enums for roles.
- Mutations: login, register, refreshToken, create/update/delete for users and posts.
- Subscriptions: `postAdded` for real-time post notifications.

### 6. Bearer Token Creation & Validation
- Tokens are created via the `login` and `register` mutations.
- The backend validates tokens automatically for protected endpoints.
- Token settings (secret, expiration) are defined in `application-dev.yml`.

### 7. Developer Workflows
- **Build/Run**: Use `gradlew bootRun` to start the app. Redis must be running locally (`redis-server`).
- **Testing**: Use Postman or GraphiQL UI (`/graphiql`) for API testing. Authenticated requests require a Bearer token.
- **Configuration**: Main configs in `application.yml` and `application-dev.yml`.
- **Database**: H2 in-memory DB for dev. Access via `/h2-console`.
- **Docker**: App and Redis can be run in Docker. See `Dockerfile` and below for details.

## Configuration Overview
- `src/main/resources/application.yml`: Base config
- `src/main/resources/application-dev.yml`: Dev profile (H2, Redis, JWT secret)
- JWT secret: `mySecretKey123456789012345678901234567890` (for dev only)
- Redis: `localhost:6379`

## Patterns & Conventions
- **GraphQL Schema**: All queries/mutations must match schema signatures in `schema.graphqls`.
- **Role-based Access**: User roles enforced in service/controller logic.
- **Error Handling**: Custom exceptions mapped to GraphQL errors.
- **Reactive Programming**: Reactor (`Mono`, `Flux`) used for async operations in security/external calls.
- **Resilience4j**: Use `@CircuitBreaker`, `@RateLimiter`, `@Retry` on service methods for fault tolerance.

## Running with Docker
If you want to run both Spring Boot and Redis with Docker:
- Use the provided `Dockerfile` for the app
- Use the official Redis image:
  ```bash
  docker run -d -p 6379:6379 redis
  ```

## Project Structure
- `controller/`: GraphQLController (API entrypoint)
- `service/`: Business logic, JWT creation/validation
- `repository/`: Data access (JPA)
- `security/`: JWT filter, config
- `resources/`: GraphQL schema, configs

## Integration Points
- **Redis**: Used for caching, must be running for full functionality.
- **JWT**: All authentication flows depend on correct JWT secret/config.
- **External Services**: Calls to external APIs are wrapped with Resilience4j for reliability.

## Useful Endpoints
- `/graphql`: Main GraphQL endpoint
- `/graphiql`: GraphiQL UI
- `/h2-console`: H2 DB console
- `/actuator/health`: Health check

## Notes
- Always start Redis before running the Spring Boot app.
- For production, change the JWT secret and use a real database.

## Example Patterns
- **JWT Validation**: See `JwtWebFilter.java` for extracting and validating tokens from requests.
- **GraphQL Mutation**: See `GraphQLController.java` for login/register mutation structure.
- **Resilience4j Usage**: See `PostService.java` for circuit breaker and retry annotations.

## License
MIT

