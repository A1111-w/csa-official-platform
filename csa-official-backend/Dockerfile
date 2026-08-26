FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S -g 10001 csa \
    && adduser -S -D -H -u 10001 -G csa csa \
    && mkdir -p /app/uploads \
    && chown -R csa:csa /app

COPY --from=build --chown=csa:csa /app/target/*.jar app.jar

USER csa

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
