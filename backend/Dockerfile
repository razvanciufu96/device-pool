# Build stage: uses the Maven Wrapper, same as a local build.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/target/backend-*.jar app.jar
# H2 database files live here; mounted as a volume in docker-compose.yml.
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
