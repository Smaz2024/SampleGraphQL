# --- Stage 1: The Builder ---
# This stage compiles the Java source code and packages it into an executable JAR.
# It uses a specific Gradle image with a full JDK, providing all necessary build tools.
# The 'as builder' syntax names this stage, allowing us to reference its output later.
FROM gradle:8.5-jdk17-alpine as builder

# Set the working directory inside the container. All subsequent commands will be run from this path.
WORKDIR /app

# --- Dependency Caching Optimization ---
# Copy only the build configuration files first. Docker caches layers, and by separating
# the rarely changing dependency configuration from the frequently changing source code,
# we can reuse the downloaded dependency layer from previous builds, dramatically speeding up build times.
COPY build.gradle settings.gradle ./
COPY gradlew ./
COPY gradle ./gradle/

# Copy the rest of the application's source code.
# This layer is invalidated more frequently, but the expensive dependency layer above remains cached.
COPY src ./src

# --- Build the Application ---
# Build the application using the Gradle wrapper.
# --mount=type=cache...: This is a modern Docker feature that provides a persistent cache for Gradle
#   dependencies (/root/.gradle). It's more efficient than traditional layer caching for build tools.
# --no-daemon: A best practice for CI/CD and containerized builds. It prevents the Gradle daemon
#   from lingering, which is unnecessary in ephemeral containers.
# -x test: Skips running tests during the image build process. Tests should be run in an earlier
#   stage of a CI/CD pipeline.
RUN --mount=type=cache,target=/root/.gradle ./gradlew build --no-daemon -x test


# --- Stage 2: The Final Production Image ---
# This stage builds the final, lightweight image that will run in production.
# It starts from a minimal Java Runtime Environment (JRE) base image, which is significantly
# smaller and has a reduced attack surface compared to a full JDK.
FROM eclipse-temurin:17-jre-alpine

# --- Security Best Practice: Create and use a Non-Root User ---
# Create a dedicated, unprivileged user and group to run the application.
# Using a static UID (e.g., 1001) is a good practice for reproducibility and security.
# Running as a non-root user follows the Principle of Least Privilege, limiting the potential
# damage if the application process is compromised.
RUN adduser -D -u 1001 appuser

# Switch to the non-root user for all subsequent commands and for running the application itself.
USER appuser

# Set the working directory for the application inside the container.
WORKDIR /home/appuser/app

# Copy only the compiled JAR file from the 'builder' stage into the final image.
# This is the core of the multi-stage build pattern. The final image contains only the
# application and its runtime dependencies, excluding all build tools and source code.
# This results in a minimal, secure, and efficient production image.
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port the application will run on. This serves as documentation for users of the image
# and can be used by container orchestrators.
EXPOSE 8080

# Define the command to run the application when a container is started.
# Using the exec form (JSON array) is the recommended practice. It makes the Java process
# the main process (PID 1) in the container, allowing it to receive OS signals like SIGTERM
# for graceful shutdown, which is essential for orchestrators like Kubernetes.
ENTRYPOINT ["java", "-jar", "app.jar"]