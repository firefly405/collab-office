FROM maven:3.9.9-eclipse-temurin-17 AS java-build
WORKDIR /workspace

# Copy build descriptors first so dependency resolution remains cached while
# application source files change.
COPY pom.xml ./
COPY collab-office-common/pom.xml collab-office-common/pom.xml
COPY collab-office-gateway/pom.xml collab-office-gateway/pom.xml
COPY collab-office-ws/pom.xml collab-office-ws/pom.xml
COPY collab-office-service/user-service/pom.xml collab-office-service/user-service/pom.xml
COPY collab-office-service/document-service/pom.xml collab-office-service/document-service/pom.xml
COPY collab-office-service/collaboration-service/pom.xml collab-office-service/collaboration-service/pom.xml
COPY collab-office-service/message-service/pom.xml collab-office-service/message-service/pom.xml
COPY collab-office-service/meeting-service/pom.xml collab-office-service/meeting-service/pom.xml
COPY collab-office-service/task-service/pom.xml collab-office-service/task-service/pom.xml
COPY collab-office-service/file-service/pom.xml collab-office-service/file-service/pom.xml
COPY collab-office-service/search-service/pom.xml collab-office-service/search-service/pom.xml
COPY collab-office-service/notification-service/pom.xml collab-office-service/notification-service/pom.xml

COPY collab-office-common/src collab-office-common/src
COPY collab-office-gateway/src collab-office-gateway/src
COPY collab-office-ws/src collab-office-ws/src
COPY collab-office-service/user-service/src collab-office-service/user-service/src
COPY collab-office-service/document-service/src collab-office-service/document-service/src
COPY collab-office-service/collaboration-service/src collab-office-service/collaboration-service/src
COPY collab-office-service/message-service/src collab-office-service/message-service/src
COPY collab-office-service/meeting-service/src collab-office-service/meeting-service/src
COPY collab-office-service/task-service/src collab-office-service/task-service/src
COPY collab-office-service/file-service/src collab-office-service/file-service/src
COPY collab-office-service/search-service/src collab-office-service/search-service/src
COPY collab-office-service/notification-service/src collab-office-service/notification-service/src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode -Dmaven.test.skip=true clean package

FROM eclipse-temurin:17-jre-alpine AS java-runtime
RUN addgroup -S collab && adduser -S -G collab collab
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom" \
    TZ=Asia/Shanghai
USER collab

FROM java-runtime AS gateway
COPY --from=java-build --chown=collab:collab /workspace/collab-office-gateway/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS websocket
COPY --from=java-build --chown=collab:collab /workspace/collab-office-ws/target/*.jar /app/app.jar
EXPOSE 9001 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS user-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/user-service/target/*.jar /app/app.jar
EXPOSE 8101
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS document-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/document-service/target/*.jar /app/app.jar
EXPOSE 8102
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS collaboration-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/collaboration-service/target/*.jar /app/app.jar
EXPOSE 8103
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS message-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/message-service/target/*.jar /app/app.jar
EXPOSE 8104
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS meeting-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/meeting-service/target/*.jar /app/app.jar
EXPOSE 8105
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS task-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/task-service/target/*.jar /app/app.jar
EXPOSE 8106
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:17-jre-jammy AS file-service
RUN sed -i \
        -e 's|http://archive.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        -e 's|http://security.ubuntu.com/ubuntu|https://mirrors.aliyun.com/ubuntu|g' \
        /etc/apt/sources.list \
    && apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
        fonts-noto-cjk \
        libreoffice-calc-nogui \
        libreoffice-impress-nogui \
        libreoffice-writer-nogui \
        netcat-openbsd \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system collab \
    && useradd --system --gid collab --home-dir /app --shell /usr/sbin/nologin collab
RUN mkdir -p /app /tmp/libreoffice-work \
    && chown -R collab:collab /app /tmp/libreoffice-work
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom" \
    TZ=Asia/Shanghai \
    HOME=/app
USER collab
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/file-service/target/*.jar /app/app.jar
EXPOSE 8107
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS notification-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/notification-service/target/*.jar /app/app.jar
EXPOSE 8109
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM java-runtime AS search-service
COPY --from=java-build --chown=collab:collab /workspace/collab-office-service/search-service/target/*.jar /app/app.jar
EXPOSE 8110
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
