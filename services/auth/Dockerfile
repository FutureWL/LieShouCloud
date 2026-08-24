# syntax=docker/dockerfile:1.7
# LieShouCloudPro · Auth Service (port 8083) multi-stage
# 与 user/admin/gateway 同样的 build context=./services 风格
# docker build context = ../../services

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 拷贝整个 services/ 让 maven 找得到 parent + sibling modules
COPY .. /workspace/
# 阿里云 Maven 镜像加速 (./services/.m2-settings.xml)
COPY ./.m2-settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -pl auth -am clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /workspace/auth/target/lieshoucloud-auth.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
USER app

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8083/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
