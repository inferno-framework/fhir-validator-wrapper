# HL7 FHIR Validation Service

The `fhir-validator-wrapper` provides a persistent service for executing the 
[FHIR® Validator](https://wiki.hl7.org/Using_the_FHIR_Validator). It is intended to provide validation capabilities
to other applications that integrate it.

## Installation

**System Requirements:** The Validation Service requires Java 1.8 or above.

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

## Running with Docker

Build
```shell script
./build_docker.sh
```
Run
```shell script
docker run -p 4567:4567 hl7_validator
```
## Contact Us
The Inferno development team can be reached by email at inferno@groups.mitre.org.  Inferno also has a dedicated [HL7 FHIR chat channel](https://chat.fhir.org/#narrow/stream/153-inferno).

## License

Copyright 2019 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
```
http://www.apache.org/licenses/LICENSE-2.0
```
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
