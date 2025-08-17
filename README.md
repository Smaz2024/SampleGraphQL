# SampleGraphQL

A Spring Boot application demonstrating a robust GraphQL API with JWT authentication, Redis caching, and Resilience4j fault tolerance. This project is ideal for learning modern backend development with Java, Spring Boot, and GraphQL.

## Features
- GraphQL API for users and posts
- JWT-based authentication and authorization
- Role-based access control (USER, ADMIN, MODERATOR)
- Redis caching for performance
- Resilience4j for circuit breaking, rate limiting, and retries
- H2 in-memory database for development
- Real-time subscriptions (WebSocket)

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

### 6. Bearer Token Creation & Validation
- Tokens are created via the `login` and `register` mutations.
- The backend validates tokens automatically for protected endpoints.
- Token settings (secret, expiration) are defined in `application-dev.yml`.

## Configuration Overview
- `src/main/resources/application.yml`: Base config
- `src/main/resources/application-dev.yml`: Dev profile (H2, Redis, JWT secret)
- JWT secret: `mySecretKey123456789012345678901234567890` (for dev only)
- Redis: `localhost:6379`

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

## Useful Endpoints
- `/graphql`: Main GraphQL endpoint
- `/graphiql`: GraphiQL UI
- `/h2-console`: H2 DB console
- `/actuator/health`: Health check

## Notes
- Always start Redis before running the Spring Boot app.
- For production, change the JWT secret and use a real database.

## License
MIT

