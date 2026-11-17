# One image recipe for every service: docker compose passes SERVICE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY common-events/pom.xml common-events/
COPY common-messaging/pom.xml common-messaging/
COPY wallet-service/pom.xml wallet-service/
COPY transfer-service/pom.xml transfer-service/
COPY notification-service/pom.xml notification-service/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline -DskipTests || true
COPY . .
ARG SERVICE
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -pl ${SERVICE} -am package -DskipTests \
 && java -Djarmode=tools -jar ${SERVICE}/target/${SERVICE}-*.jar extract --layers --launcher --destination /app

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/snapshot-dependencies/ ./
COPY --from=build /app/application/ ./
USER app
# Small heap: three JVMs plus Kafka and MySQL must fit on a laptop.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
