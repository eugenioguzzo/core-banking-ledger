# ---- Build stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cached separately so dependency downloads are skipped when only source files change.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/target/core-banking-ledger.jar app.jar
USER app

# Render injects PORT at runtime; application.yml reads it via ${PORT:8080}.
EXPOSE 8080

# Tuned for a 512 MB container (e.g. Render's free plan): caps the heap well below the
# container limit (leaving room for metaspace, threads and native/direct memory), and
# uses the serial collector, which has a much smaller memory footprint than the default
# G1 collector on small heaps.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -Xss512k"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
