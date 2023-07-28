package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.unmap;

import java.io.IOException;

import com.google.gson.Gson;
import org.mitre.inferno.FHIRPathEvaluator;
import org.mitre.inferno.Validator;
import spark.ResponseTransformer;

import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.CodeType;

public class Endpoints {
  public static final ResponseTransformer TO_JSON = new Gson()::toJson;
  private static Endpoints endpoints = null;

  private final Validator validator;
  private final FHIRPathEvaluator pathEvaluator;

  private Endpoints(Validator validator, FHIRPathEvaluator evaluator) {
    this.validator = validator;
    this.pathEvaluator = evaluator;
    createRoutes();
  }

  /**
   * This adds permissive CORS headers to all requests.
   */
  private static void setHeaders() {

    before((req, res) -> {
      res.header("Access-Control-Allow-Origin", "*");
      res.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
      res.header("Access-Control-Allow-Headers", "*");
    });

    // This responds to OPTIONS requests, used by browsers to "preflight" check CORS
    // requests,
    // with a 200 OK response with no content and the CORS headers above
    options("*", (req, res) -> "");

  }

  /**
   * Create a wait message while the validator loads.
   * 
   * @throws Exception
   */
  private static String generateWaitMessage() throws Exception {

    OperationOutcome.IssueSeverity sev = OperationOutcome.IssueSeverity.ERROR;
    OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent(
        sev,
        IssueType.INCOMPLETE);
    String message = "Validator still loading... please wait.";
    issue.setDiagnostics(message);
    issue.setDetails(new CodeableConcept().setText(message));
    issue.addExtension(
        "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-line",
        new IntegerType(1));
    issue.addExtension(
        "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-col",
        new IntegerType(1));
    issue.addExtension(
        "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-source",
        new CodeType("ValidationService"));
    OperationOutcome oo = new OperationOutcome(issue);
    return new JsonParser().composeString(oo);
  }

  /**
   * Set up temporary routes while the validator still loads.
   * 
   * @param port the port on which to listen for requests
   */
  public static void setupLoadingRoutes(int port) {
    port(port);
    setHeaders();

    get("*", (req, res) -> {
      res.type("application/fhir+json");
      res.status(503);
      res.body("Validator still loading... please wait.");
      return generateWaitMessage();
    });
    post("*", (req, res) -> {
      res.type("application/fhir+json");
      res.status(503);
      res.body("Validator still loading... please wait.");
      return generateWaitMessage();
    });
    put("*", (req, res) -> {
      res.type("application/fhir+json");
      res.status(503);
      res.body("Validator still loading... please wait.");
      return generateWaitMessage();
    });
  }

  /**
   * Remove temporary routes set. This is meant to be called once the validator
   * has been loaded.
   */
  public static void teardownLoadingRoutes() {
    unmap("*", "get");
    unmap("*", "post");
    unmap("*", "put");
  }

  /**
   * Get the existing Endpoints or create one if it does not already exist.
   *
   * @param validator the Validator that should be used at the /validator
   *                  endpoint.
   *                  Passing null will skip setting up the /validator endpoint.
   * @param evaluator the FHIRPathEvaluator that should be used at the /fhirpath
   *                  endpoint.
   *                  Passing null will skip setting up the /fhirpath endpoint.
   * @param port      the port to listen for requests on
   * @return the singleton Endpoints
   */
  public static Endpoints getInstance(Validator validator, FHIRPathEvaluator evaluator, int port) {
    teardownLoadingRoutes();
    if (endpoints == null) {
      endpoints = new Endpoints(validator, evaluator);
    }
    return endpoints;
  }

  /**
   * Creates the API routes for receiving and processing HTTP requests from
   * clients.
   */
  private void createRoutes() {

    if (validator != null) {
      ValidatorEndpoint.getInstance(validator);
    }

    if (pathEvaluator != null) {
      FHIRPathEndpoint.getInstance(pathEvaluator);
    }
  }

}
