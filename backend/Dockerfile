# Build: the Maven wrapper resolves dependencies first, so source-only changes reuse that layer.
# Tests are skipped here because they need Docker themselves (Testcontainers); CI runs them.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 portal
COPY --from=build /app/target/portal-backend-*.jar app.jar
USER portal
ENV TZ=Europe/Paris
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
