FROM openjdk:8 AS build
WORKDIR /home
# Grab Gradle first so it can be cached
COPY gradle gradle
COPY gradlew .
RUN ./gradlew --version

COPY settings.gradle .
COPY gradle.properties .
COPY build.gradle.kts .
COPY config config
COPY igs igs
COPY src src

RUN ./gradlew build
RUN tar -xvf build/distributions/InfernoValidationService-*.tar

FROM openjdk:8
WORKDIR /home
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

COPY --from=build /home/InfernoValidationService-* .
COPY igs igs
RUN bin/InfernoValidationService prepare
EXPOSE 4567

CMD ["./bin/InfernoValidationService"]