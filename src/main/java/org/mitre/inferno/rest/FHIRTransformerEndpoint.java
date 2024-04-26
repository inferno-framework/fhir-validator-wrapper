package org.mitre.inferno.rest;

import static spark.Spark.post;

import org.mitre.inferno.FHIRTransformer;

  /**
  * REST Endpoint that deals with FHIR Transformations
  */
  public class FHIRTransformerEndpoint {
    private static FHIRTransformerEndpoint fhirTransformerEndpoint = null;
    private final FHIRTransformer transformer;

    private FHIRTransformerEndpoint(FHIRTransformer transformer) {
      this.transformer = transformer;
      createRoutes();
    }

    /**
     * Get the existing FHIRTransformerEndpoint or create one if it does not already exist.
     *
     * @param transformer the FHIRTransformerEndpoint that should be used for this endpoint
     * @return the singleton FHIRTransformerEndpoint
     */
    public static FHIRTransformerEndpoint getInstance(FHIRTransformer transformer) {
      if (fhirTransformerEndpoint == null) {
          fhirTransformerEndpoint = new FHIRTransformerEndpoint(transformer);
      }
      return fhirTransformerEndpoint;
    }

    /**
     * Creates the API routes for receiving and processing requests for the FHIRTransformer service.
     */
    private void createRoutes() {
      // we add a post path for "transform" - which allows us to go between xml<>json
      post("/transform",
          (req, res) -> {            
            return transformer.transformResource(req.body(), req, res);
          });
    }

}