# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /app
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-noble
RUN groupadd --system --gid 65532 app && useradd --system --uid 65532 --gid app app
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
