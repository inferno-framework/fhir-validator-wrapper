# Validator Routes

### Validate a resource
- **Route:**
`POST /validator/validate`
- **Query Params:**
`profile=[comma separated list of profile URLs]` (Required)
- **Body:**
the JSON or XML FHIR resource to validate
- **Response:**
a JSON [OperationOutcome](https://www.hl7.org/fhir/operationoutcome.html)

### List supported resources
- **Route:**
`GET /validator/resources`
- **Response:**
a JSON array of FHIR resource types known to the validator

### List supported profiles
- **Route:**
`GET /validator/profiles`
- **Response:**
a JSON array of [profile URLs](http://www.hl7.org/fhir/structuredefinition-definitions.html#StructureDefinition.url) known to the validator

### Load a custom profile
- **Route:**
`POST /validator/profiles`
- **Body:**
the JSON or XML [profile](http://www.hl7.org/fhir/structuredefinition.html) you want to load into the validator
- **Response:**
None

### List profiles by implementation guide (IG)
- **Route:**
`GET /validator/profiles-by-ig`
- **Response:**
a JSON object containing an array of profile URLs for each IG loaded into the validator

### List known IGs
- **Route:**
`GET /validator/igs`
- **Response:**
a JSON object containing the canonical URL for each IG loaded into the validator

### Load an IG by NPM package ID
- **Route:**
`PUT /validator/igs/[NPM package ID]`
- **Query Params:**
`version=[NPM package version]` (Optional)
- **Response:**
the NPM ID, version, and list of profile URLs of the loaded IG. [See here](#loading-an-ig-by-id-and-version) for an example.

### Load a custom IG
- **Route:**
`POST /validator/igs`
- **Body:**
the raw contents of the `package.tgz` containing the IG to be loaded into the validator.
Note that the request must have the `Content-Encoding: gzip` header.
- **Response:**
the NPM ID, version, and list of profile URLs of the loaded IG. [See here](#loading-a-custom-ig) for an example.

# FHIRPath Routes

### Evaluate a FHIRPath expression
- **Route:**
`POST /fhirpath/evaluate`
- **Query Params:**
`path=[FHIRPath expression]` (Required)
- **Body:**
the JSON or XML FHIR resource to serve as the root resource when evaluating the expression
- **Response:**
a JSON array representing the result of evaluating the given expression against the given root resource

# Example Requests and Responses

### Loading an IG by ID and version
This request assumes that the service is listening on port 4567.

- **Example Request:**
`curl -s -X PUT 'http://localhost:4567/validator/igs/hl7.fhir.us.qicore?version=4.9.0'`
- **Example Response:**
```
{
    "id": "hl7.fhir.us.qicore",
    "version": "4.9.0",
    "profiles": [
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-adverseevent",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-allergyintolerance",
        ...
    ]
}
```

### Loading a custom IG
This request assumes that the service is listening on port 4567 and that the `package.tgz` to upload is
in the current working directory.

- **Example Request:**
`curl -s -X POST -H 'Content-Encoding: gzip' --data-binary '@package.tgz' 'http://localhost:4567/validator/igs'`
- **Example Response:**
```
{
    "id": "example.fhir.core",
    "version": "1.0.0",
    "profiles": [
        "http://example.org/fhir/example/StructureDefinition/example-condition",
        "http://example.org/fhir/example/StructureDefinition/example-patient"
    ]
}
```
