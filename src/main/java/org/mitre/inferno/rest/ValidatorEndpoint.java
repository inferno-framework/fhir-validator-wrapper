package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.post;
import static spark.Spark.port;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.hl7.fhir.r5.elementmodel.Manager.FhirFormat;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Resource;
import org.mitre.inferno.Validator;

public class ValidatorEndpoint {
  private static ValidatorEndpoint validatorEndpoint = null;
  private final Validator validator;

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
      res.header("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      res.header("Access-Control-Allow-Headers", "Access-Control-Allow-Origin, Content-Type");
    });

    // This responds to OPTIONS requests, used by browsers to "preflight" check CORS requests, with a 200 OK response with no content and the CORS headers above
    options("*",
        (req, res) -> {
          return "";
        });

    post("/validate",
        (req, res) -> {
          res.type("application/fhir+json");
          return validateResource(req.bodyAsBytes(), req.queryParams("profile"));
        });

    get("/resources",
        (req, res) -> {
          res.type("application/json");
          return resourcesList();
        });

    get("/profiles",
        (req, res) -> {
          res.type("application/json");
          return getProfiles();
        });

    get("/igs",
        (req, res) -> {
          res.type("application/json");
          return getIGs();
        });

    post("/profile",
        (req, res) -> {
          byte[] profile = req.bodyAsBytes();
          try {
            loadProfile(profile);
            res.status(200);
            return "";
          } catch (IOException e) {
            res.status(500);
            return "";
          }
        });
  }

  /**
   * Handles loading FHIR profiles into the validator.
   *
   * @param profile the FHIR profile to be loaded
   * @throws IOException if the profile could not be loaded
   */
  private void loadProfile(byte[] profile) throws IOException {
    FhirFormat fmt = FormatUtilities.determineFormat(profile);
    Resource resource = FormatUtilities.makeParser(fmt).parse(profile);
    validator.loadProfile(resource);
  }

  /**
   * Handles returning the currently loaded profiles in the validator.
   *
   * @return a list of profile URLs
   */
  private String getProfiles() {
    LinkedHashSet<String> structuresDeduplicated = new LinkedHashSet<String>();
    List<String> structures = new ArrayList<String>();
    structuresDeduplicated.addAll(validator.getStructures());
    structures.addAll(structuresDeduplicated);
    Collections.sort(structures);
    return new Gson().toJson(structures);
  }

  /**
   * Handles returning a list of available IGs that could be loaded.
   *
   * @return a list of IG URLs
   */
  private String getIGs() throws IOException {
    return new Gson().toJson(validator.getKnownIGs());
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
    ArrayList<String> patientProfiles = new ArrayList<String>(Arrays.asList(profile.split(",")));
    OperationOutcome oo = validator.validate(resource, patientProfiles);
    return resourceToJson(oo);
  }

  /**
   * Returns the list of FHIR Resources supported by the validator.
   * @return
   */
  private String resourcesList() {
    LinkedHashSet<String> resourcesDeduplicated = new LinkedHashSet<String>();
    resourcesDeduplicated.addAll(validator.getResources());
    return new Gson().toJson(resourcesDeduplicated);
  }

  /**
   * Serializes a FHIR resource to its JSON representation.
   * @param resource the resource to be serialized
   * @return the serialized FHIR resource
   * @throws IOException if the resource fails to be serialized
   */
  private String resourceToJson(Resource resource) throws IOException {
    IParser parser = new JsonParser();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    parser.compose(baos, resource);
    return baos.toString();
  }
}
