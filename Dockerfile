FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --uid 10001 --create-home spring
COPY --from=build --chown=spring:spring /workspace/target/dental-crm-*.jar /app/application.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
