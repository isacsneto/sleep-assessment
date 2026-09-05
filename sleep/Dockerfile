FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY build.gradle gradlew settings.gradle ./
COPY gradle/ gradle/

RUN ./gradlew wrapper

COPY src/ src

# Tests run against a real Postgres via Testcontainers, which needs a Docker
# daemon. That is not available during an image build, so build only the jar
# here; the test suite runs in CI / locally via `./gradlew test`.
RUN ./gradlew bootJar -x test

ENTRYPOINT ["java","-jar","build/libs/sleep-0.0.1-SNAPSHOT.jar"]
