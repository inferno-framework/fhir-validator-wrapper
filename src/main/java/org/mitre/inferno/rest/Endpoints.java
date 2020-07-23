package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.port;

import com.google.gson.Gson;
import org.hl7.fhir.r5.utils.FHIRPathEngine;
import org.mitre.inferno.Validator;
import spark.ResponseTransformer;

public class Endpoints {
  public static final ResponseTransformer TO_JSON = new Gson()::toJson;
  private static Endpoints endpoints = null;

  private final Validator validator;
  private final FHIRPathEngine pathEngine;

  private Endpoints(Validator validator, FHIRPathEngine engine, int portNum) {
    this.validator = validator;
    this.pathEngine = engine;
    port(portNum);
    createRoutes();
  }

  /**
   * Get the existing Endpoints or create one if it does not already exist.
   *
   * @param validator the Validator that should be used at the /validator endpoint.
   *                  Passing null will skip setting up the /validator endpoint.
   * @param engine the FHIRPathEngine that should be used at the /fhirpath endpoint.
   *               Passing null will skip setting up the /fhirpath endpoint.
   * @return the singleton Endpoints
   */
  public static Endpoints getInstance(Validator validator, FHIRPathEngine engine, int portNum) {
    if (endpoints == null) {
      endpoints = new Endpoints(validator, engine, portNum);
    }
    return endpoints;
  }

  /**
   * Creates the API routes for receiving and processing HTTP requests from clients.
   */
  private void createRoutes() {
    // This adds permissive CORS headers to all requests
    before((req, res) -> {
      res.header("Access-Control-Allow-Origin", "*");
      res.header("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
      res.header("Access-Control-Allow-Headers", "Access-Control-Allow-Origin, Content-Type");
    });

    // This responds to OPTIONS requests, used by browsers to "preflight" check CORS requests,
    // with a 200 OK response with no content and the CORS headers above
    options("*", (req, res) -> "");

    if (validator != null) {
      path("/validator", () -> ValidatorEndpoint.getInstance(validator));
    }

    if (pathEngine != null) {
      path("/fhirpath", () -> FHIRPathEndpoint.getInstance(pathEngine));
    }
  }

}
