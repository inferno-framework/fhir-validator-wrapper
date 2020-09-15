package org.mitre.inferno.rest;

import static spark.Spark.post;

import java.io.IOException;
import org.hl7.fhir.r4.model.Base;
import org.mitre.inferno.FHIRPathEvaluator;
import org.mitre.inferno.JsonParser;

public class FHIRPathEndpoint {
  private static FHIRPathEndpoint fhirPathEndpoint = null;
  private final FHIRPathEvaluator pathEvaluator;

  private FHIRPathEndpoint(FHIRPathEvaluator evaluator) {
    this.pathEvaluator = evaluator;
    createRoutes();
  }

  /**
   * Get the existing FHIRPathEndpoint or create one if it does not already exist.
   *
   * @param evaluator the FHIRPathEvaluator that should be used for this endpoint
   * @return the singleton FHIRPathEndpoint
   */
  public static FHIRPathEndpoint getInstance(FHIRPathEvaluator evaluator) {
    if (fhirPathEndpoint == null) {
      fhirPathEndpoint = new FHIRPathEndpoint(evaluator);
    }
    return fhirPathEndpoint;
  }

  /**
   * Creates the API routes for receiving and processing requests for the FHIRPath service.
   */
  private void createRoutes() {
    post("/evaluate", (req, res) -> {
      res.type("application/fhir+json");
      return evaluate(req.body(), req.queryParams("type"), req.queryParams("path"));
    });
  }

  private String evaluate(String body, String type, String path) throws IOException {
    Base rootElement = new JsonParser().parse(body, type);
    return pathEvaluator.evaluateToString(rootElement, path);
  }
}
