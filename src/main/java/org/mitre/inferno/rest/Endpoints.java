package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.options;
import static spark.Spark.port;

import com.google.gson.Gson;

import java.io.IOException;

import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.mitre.inferno.FHIRPathEvaluator;
import org.mitre.inferno.Validator;

import spark.ResponseTransformer;

public class Endpoints {
  public static final ResponseTransformer TO_JSON = new Gson()::toJson;
  private static Endpoints endpoints = null;

  private final Validator validator;
  private final FHIRPathEvaluator pathEvaluator;

  private Endpoints(Validator validator, FHIRPathEvaluator evaluator, int port) {
    this.validator = validator;
    this.pathEvaluator = evaluator;
    port(port);
    createRoutes();
  }

  /**
   * Get the existing Endpoints or create one if it does not already exist.
   *
   * @param validator the Validator that should be used at the /validator endpoint.
   *                  Passing null will skip setting up the /validator endpoint.
   * @param evaluator the FHIRPathEvaluator that should be used at the /fhirpath endpoint.
   *                  Passing null will skip setting up the /fhirpath endpoint.
   * @param port the port to listen for requests on
   * @return the singleton Endpoints
   */
  public static Endpoints getInstance(Validator validator, FHIRPathEvaluator evaluator, int port) {
    if (endpoints == null) {
      endpoints = new Endpoints(validator, evaluator, port);
    }
    return endpoints;
  }

  /**
   * Creates the API routes for receiving and processing HTTP requests from clients.
   */
  private void createRoutes() {
    // This adds global exception handling.
    // Note: it currently does not distinguish between 4xx and 5xx type errors,
    // and the spark framework does not catch all Throwables/Errors (eg, OOM).
    exception(Exception.class, (e, req, res) -> {
      res.type("application/fhir+json");
      OperationOutcome oo = Validator.exceptionToOperationOutcome(e);
      String body;
      try {
        body = new JsonParser().composeString(oo);
      } catch (IOException e1) {
        // "should never happen"
        // for now just wrap some text in quotes so it's valid JSON.
        res.status(500);
        body = "\"Exception occurred in handler. Original exception was: " + e.getMessage() + '"';
      }
      res.body(body);
    });

    // This adds permissive CORS headers to all requests
    before((req, res) -> {
      res.header("Access-Control-Allow-Origin", "*");
      res.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
      res.header("Access-Control-Allow-Headers", "*");
    });

    // This responds to OPTIONS requests, used by browsers to "preflight" check CORS requests,
    // with a 200 OK response with no content and the CORS headers above
    options("*", (req, res) -> "");

    if (validator != null) {
      ValidatorEndpoint.getInstance(validator);
    }

    if (pathEvaluator != null) {
      FHIRPathEndpoint.getInstance(pathEvaluator);
    }
  }

}
