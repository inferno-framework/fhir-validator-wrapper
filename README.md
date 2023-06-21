# HL7® FHIR® Validation Service

The `inferno-framework/fhir-validator-wrapper` provides a persistent service for
executing the [HL7® FHIR®
Validator](https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Validator),
which historically only was available as a Java library and a CLI-based tool.
This lightweight wrapper enables applications not implemented in Java, such as
the [Inferno Framework](https://inferno-framework.github.io), to interface with
the HL7 Validator in a service environment.  It is primarily being used within
the Inferno Framework to provide FHIR validation services for tests, as well as
to provide a [simple web-based
UI](https://github.com/inferno-framework/fhir-validator-app) for validating FHIR
resources.

Since this is just a lightweight wrapper around the HL7 FHIR Validator, most of
the functionality provided by this service is [implemented within the HL7 FHIR
Validator](https://github.com/hapifhir/org.hl7.fhir.core), which is
developed and maintained independently of this project.

The team that maintains the HL7 FHIR Validator has since created [their
own service API](https://github.com/hapifhir/org.hl7.fhir.validator-wrapper) for
the HL7 FHIR validator, making this wrapper service redundant.  This GitHub
project may be retired in favor of using that service, but will be maintained as
long as the Inferno set of tools continues to use it.

## REST API

**[See here](rest-api.md) for the REST API documentation.**

## Installation

**System Requirements:** The Validation Service requires Java 11 or above.

## Running Locally with Java

To build and run the test suite:

### *nix

```shell script
./gradlew build check test
```

### Windows

```shell script
gradlew.bat build check test
```

To run the app:

```shell script
./gradlew run
```

The port can also be set through the environment

```shell script
VALIDATOR_PORT=8080 ./gradlew run
```

## Running with Docker

Build


```shell script
./build_docker.sh
```

Run

```shell script
docker run -p 4567:4567 hl7_validator
```

Run with a different terminology server:

```shell script
docker run -p 4567:4567 --env TX_SERVER_URL=http://mytx.org/r4 hl7_validator
```

Run without terminology validation:

```shell script
docker run -p 4567:4567 --env DISABLE_TX=true hl7_validator
```

By default, the validator will return errors when a code display doesn't match the expected value from the terminology server. To return warnings instead:

```shell script
docker run -p 4567:4567 --env DISPLAY_ISSUES_ARE_WARNINGS=true hl7_validator
```

## Creating an Uber Jar

An uber jar can be created with:

```shell
./gradlew uberJar
```

By default, the uber jar will be located in `build/lib/`.

This uber jar can be executed with `java -jar InfernoValidationService-<version>-uber.jar`

## Contact Us

The Inferno development team can be reached by email at
inferno@groups.mitre.org.  Inferno also has a dedicated [HL7 FHIR chat
channel](https://chat.fhir.org/#narrow/stream/153-inferno).

## License

Copyright 2023 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
```
http://www.apache.org/licenses/LICENSE-2.0
```
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Trademark Notice

HL7, FHIR and the FHIR [FLAME DESIGN] are the registered trademarks of Health
Level Seven International and their use does not constitute endorsement by HL7.
