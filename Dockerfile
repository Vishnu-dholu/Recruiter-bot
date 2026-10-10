# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy the Maven wrapper and pom first: dependencies are downloaded in their
# own layer, so they're cached and not re-downloaded on every code change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
# -DskipTests: the generated contextLoads() test needs a running database,
# which doesn't exist while the image is being built.
RUN ./mvnw package -DskipTests -B

# ---------- Stage 2: run it ----------
# Only a JRE and the jar: a smaller image with no build tools or source code.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]