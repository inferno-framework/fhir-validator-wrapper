package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.mitre.inferno.Validator;
import spark.ResponseTransformer;

public class ValidatorEndpoint {
  private static ValidatorEndpoint validatorEndpoint = null;
  private final Validator validator;
  private static final ResponseTransformer TO_JSON = new Gson()::toJson;

  private ValidatorEndpoint(Validator validator, int portNum) {
    this.validator = validator;
    port(portNum);
    createRoutes();
  }

  /**
   * Get the existing ValidatorEndpoint or create one if it does not already exist.
   *
   * @param validator the Validator that should be used for this endpoint
   * @param portNum the port that the ValidatorEndpoint should listen on
   * @return the singleton ValidatorEndpoint
   */
  public static ValidatorEndpoint getInstance(Validator validator, int portNum) {
    if (validatorEndpoint == null) {
      validatorEndpoint = new ValidatorEndpoint(validator, portNum);
    }
    return validatorEndpoint;
  }

  /**
   * Creates the API routes for receiving and processing HTTP requests from clients.
   */
  private void createRoutes() {
    // This adds permissive CORS headers to all requests
    before((req, res) -> {
      res.header("Access-Control-Allow-Origin", "*");
      res.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
      res.header("Access-Control-Allow-Headers", "Access-Control-Allow-Origin, Content-Type, Content-Encoding");
      res.type("application/json");
    });

    // This responds to OPTIONS requests, used by browsers to "preflight" check CORS requests,
    // with a 200 OK response with no content and the CORS headers above
    options("*", (req, res) -> "");

    post("/validate",
        (req, res) -> {
          res.type("application/fhir+json");
          return validateResource(req.bodyAsBytes(), req.queryParams("profile"));
        });

    get("/resources", (req, res) -> validator.getResources(), TO_JSON);

    get("/profiles", (req, res) -> validator.getStructures(), TO_JSON);

    post("/profiles",
        (req, res) -> {
          validator.loadProfile(req.bodyAsBytes());
          return "";
        });

    get("/profiles-by-ig", (req, res) -> validator.getProfilesByIg(), TO_JSON);

    get("/igs", (req, res) -> validator.getKnownIGs(), TO_JSON);

    post("/igs",
        (req, res) -> {
          validator.loadPackage(req.bodyAsBytes());
          return "";
        });

    put("/igs/:id", (req, res) -> validator.loadIg(req.params("id")), TO_JSON);
  }

  /**
   * Handles validating resources against a profile.
   *
   * @param resource the resource to be validated
   * @param profile the profile to validate the resource against
   * @return the validation res
   * @throws Exception if the resource cannot be loaded or validated
   */
  private String validateResource(byte[] resource, String profile) throws Exception {
    List<String> patientProfiles = Arrays.asList(profile.split(","));
    OperationOutcome oo = validator.validate(resource, patientProfiles);
    return new JsonParser().composeString(oo);
  }
}
