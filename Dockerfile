FROM gradle:8.14.2-jdk21 AS build

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

RUN ./gradlew build -x test --no-daemon

FROM amazoncorretto:21.0.7-alpine3.19

WORKDIR /app

COPY --from=build /app/build/libs/*.jar /app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]