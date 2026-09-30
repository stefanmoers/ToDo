# Build-Stufe: Maven Wrapper lädt die feste Maven-Version, Java-Version ist an das Image gebunden.
FROM eclipse-temurin:21.0.7_6-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/backend-*.jar app.jar

# Laufzeit-Stufe: nur JRE, unprivilegierter Benutzer.
FROM eclipse-temurin:21.0.7_6-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
