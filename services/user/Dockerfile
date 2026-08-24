# syntax=docker/dockerfile:1.7
# LieShouCloudPro · User Service (port 8081) multi-stage
# docker build context = ../../services

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY .. /workspace/

COPY ./.m2-settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -pl user -am clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /workspace/user/target/lieshoucloud-user.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
USER app

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8081/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
