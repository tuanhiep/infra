FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/cdn-edge-cache-0.0.1-SNAPSHOT.jar /app/origin.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/origin.jar"]
