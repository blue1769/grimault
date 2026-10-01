FROM eclipse-temurin:17-jre-alpine
WORKDIR /grimault-api
COPY artifact-snapshot.jar grimault-api.jar
ENTRYPOINT ["java", "-jar", "/grimault-api/grimault-api.jar"]