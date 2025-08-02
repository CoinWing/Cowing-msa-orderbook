FROM gradle:8.14.2-jdk21 AS build

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY src ./src

RUN gradle build -x test --no-daemon

FROM amazoncorretto:21.0.7-alpine3.19

WORKDIR /app

ARG ID=1001
RUN addgroup --gid ${ID} javauser && \
    adduser --uid ${ID} --ingroup javauser --no-create-home --disabled-password javauser

COPY --from=build /app/build/libs/*.jar /app.jar

EXPOSE 8081

USER javauser

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "/app.jar"]