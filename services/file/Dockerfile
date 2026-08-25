# syntax=docker/dockerfile:1.7
# LieShouCloudPro · File Service (port 8099) multi-stage
# 文件服务（上传/下载/预览 · 强制 X-Tenant-Id · 本地磁盘存储）
# docker build context = ../../services

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY .. /workspace/

COPY ./.m2-settings.xml /root/.m2/settings.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -Dmaven.test.skip=true -pl file -am clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=builder /workspace/file/target/lieshoucloud-file.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Dfile.encoding=UTF-8"
USER app

EXPOSE 8099

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8099/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
