# Validator Routes

### Validate a resource
- **Route:**
`POST /validate`
- **Query Params:**
`profile=[comma separated list of profile URLs]` (Required)
- **Body:**
the JSON or XML FHIR resource to validate
- **Response:**
a JSON [OperationOutcome](https://www.hl7.org/fhir/operationoutcome.html)

### List supported resources
- **Route:**
`GET /resources`
- **Response:**
a JSON array of FHIR resource types known to the validator

### List supported profiles
- **Route:**
`GET /profiles`
- **Response:**
a JSON array of [profile URLs](http://www.hl7.org/fhir/structuredefinition-definitions.html#StructureDefinition.url) known to the validator

### Load a custom profile
- **Route:**
`POST /profiles`
- **Body:**
the JSON or XML [profile](http://www.hl7.org/fhir/structuredefinition.html) you want to load into the validator
- **Response:**
None

### List profiles by implementation guide (IG)
- **Route:**
`GET /profiles-by-ig`
- **Response:**
a JSON object containing an array of profile URLs for each IG loaded into the validator

### List known IGs
- **Route:**
`GET /igs`
- **Response:**
a JSON object containing the canonical URL for each IG loaded into the validator

### Load an IG by NPM package ID
- **Route:**
`PUT /igs/[NPM package ID]`
- **Query Params:**
`version=[NPM package version]` (Optional)
- **Response:**
the NPM ID, version, and list of profile URLs of the loaded IG. [See here](#loading-an-ig-by-id-and-version) for an example.

### Load a custom IG
- **Route:**
`POST /igs`
- **Body:**
the raw contents of the `package.tgz` tarball containing the IG to be loaded into the validator.  
**The `package.tgz` contents must conform to the [NPM Package Specification](https://confluence.hl7.org/display/FHIR/NPM+Package+Specification)
and must include the .index.json file.**  
Also note that the request must have the `Content-Encoding: gzip` header.
- **Response:**
the NPM ID, version, and list of profile URLs of the loaded IG. [See here](#loading-a-custom-ig) for an example.

### Get this wrapper's version
- **Route:**
`GET /version`
- **Response:**
A JSON object containing identifiers and version numbers for the software in use, such as the version of the wrapper itself and the version of the HL7 validator.
- **Example Response:**
```
{"org.hl7.fhir.validation":"5.6.93","inferno-framework/fhir-validator-wrapper":"2.2.1"}
```


# FHIRPath Routes

### Evaluate a FHIRPath expression
- **Route:**
`POST /evaluate`
- **Query Params:**
`path=[FHIRPath expression]` (Required)
- **Body:**
the JSON or XML FHIR element to serve as the root element when evaluating the expression. Accepts all
complex datatypes (i.e. full resources, Elements, BackboneElements) but not primitives.
- **Response:**
a JSON array representing the result of evaluating the given expression against the given root element.
Each "result" in the returned array will be in the form
`{ "type": "[FHIR datatype name]", "element": [JSON representation of element] }`.

# Example Requests and Responses

### Loading an IG by ID and version
This request assumes that the service is listening on port 4567.

- **Example Request:**
`curl -s -X PUT 'http://localhost:4567/igs/hl7.fhir.us.qicore?version=4.9.0'`
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
`curl -s -X POST -H 'Content-Encoding: gzip' --data-binary '@package.tgz' 'http://localhost:4567/igs'`
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
