package org.mitre.inferno.rest;

import static spark.Spark.get;
import static spark.Spark.post;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
  private static Validator validator = null;

  private ValidatorEndpoint() {
    getValidator();
    createRoutes();
  }

  /**
   * Get the existing validatorEndpoint or create one if it does not already exist.
   *
   * @return
   */
  public static ValidatorEndpoint getInstance() {
    if (validatorEndpoint == null) {
      validatorEndpoint = new ValidatorEndpoint();
    }
    return validatorEndpoint;
  }

  /**
   * Creates the API routes for receiving and processing HTTP requests from clients.
   */
  private void createRoutes() {
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
  private String getIGs() {
    return new Gson().toJson(validator.getKnownIGs());
  }

  /**
   * Returns the validator associated with this ValidatorEndpoint.
   * <p>Creates a new Validator if one has not yet been created.</p>
   *
   * @return
   */
  private Validator getValidator() {
    if (validator == null) {
      validator = new Validator("./igs/package");
    }
    return validator;
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
    ArrayList<String> patientProfiles = new ArrayList<>();
    patientProfiles.add(profile);
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
