FROM openjdk:8 AS build

# RUN curl -ksSL https://gitlab.mitre.org/mitre-scripts/mitre-pki/raw/master/os_scripts/install_certs.sh | JAVA_HOME=/usr/local/openjdk-8 MODE=java sh
# If using under Zscalar, comment out the above and run using: 'docker build --no-cache -t hl7_validator .'

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