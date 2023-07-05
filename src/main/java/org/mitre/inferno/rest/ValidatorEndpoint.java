package org.mitre.inferno.rest;

import static org.mitre.inferno.rest.Endpoints.TO_JSON;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.mitre.inferno.Validator;

public class ValidatorEndpoint {
  private static ValidatorEndpoint validatorEndpoint = null;
  private final Validator validator;

  private ValidatorEndpoint(Validator validator) {
    this.validator = validator;
    createRoutes();
  }

  /**
   * Get the existing ValidatorEndpoint or create one if it does not already exist.
   *
   * @param validator the Validator that should be used for this endpoint
   * @return the singleton ValidatorEndpoint
   */
  public static ValidatorEndpoint getInstance(Validator validator) {
    if (validatorEndpoint == null) {
      validatorEndpoint = new ValidatorEndpoint(validator);
    }
    return validatorEndpoint;
  }

  /**
   * Creates the API routes for receiving and processing requests for the validation service.
   */
  private void createRoutes() {
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

    post("/igs", (req, res) -> validator.loadPackage(req.bodyAsBytes()), TO_JSON);

    put("/igs/:id",
        (req, res) -> validator.loadIg(req.params("id"), req.queryParams("version")),
        TO_JSON);

    get("/version", (req, res) -> buildVersionResponse(), TO_JSON);
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
    List<String> patientProfiles;
    if (profile != null) {
      patientProfiles = Arrays.asList(profile.split(","));
    } else {
      patientProfiles = new ArrayList<String>();
    }

    OperationOutcome oo = validator.validate(resource, patientProfiles);
    return new JsonParser().composeString(oo);
  }

  /**
   * Build a Map of the library versions used by this validator.
   *
   * @return a Map of library identifier -> version string
   */
  private Map<String,String> buildVersionResponse() {
    // full package names used here only to make it more obvious what's going on
    // since the class names aren't distinct enough
    String hl7ValidatorVersion = org.hl7.fhir.validation.cli.utils.VersionUtil.getVersion();
    String wrapperVersion = org.mitre.inferno.Version.getVersion();

    Map<String, String> versions = new HashMap<>();
    versions.put("org.hl7.fhir.validation", hl7ValidatorVersion);
    versions.put("inferno-framework/fhir-validator-wrapper", wrapperVersion);
    return versions;
  }
}
