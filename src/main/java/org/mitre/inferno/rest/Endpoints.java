package org.mitre.inferno.rest;

import static spark.Spark.before;
import static spark.Spark.options;
import static spark.Spark.port;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.unmap;

import com.google.gson.Gson;
import org.mitre.inferno.FHIRPathEvaluator;
import org.mitre.inferno.Validator;
import spark.ResponseTransformer;
import com.google.gson.JsonObject;

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
   * Set up temporary routes while the validator still loads.
   * 
   * @param port the port to listen for requests on
   */
  public static void setupLoadingRoutes(int port) {
    port(port);
    setHeaders();

    JsonObject warning = new JsonObject();
    warning.addProperty("Warning", "Validator still loading... please wait.");

    get("*", (req, res) -> {
      res.type("application/fhir+json");
      return warning;
    });
    post("*", (req, res) -> {
      res.type("application/fhir+json");
      return warning;
    });
    put("*", (req, res) -> {
      res.type("application/fhir+json");
      return warning;
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
    setHeaders();

    if (validator != null) {
      ValidatorEndpoint.getInstance(validator);
    }

    if (pathEvaluator != null) {
      FHIRPathEndpoint.getInstance(pathEvaluator);
    }
  }

}
