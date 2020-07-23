package org.mitre.inferno.rest;

import org.hl7.fhir.r5.utils.FHIRPathEngine;

public class FHIRPathEndpoint {
  private static FHIRPathEndpoint fhirPathEndpoint = null;
  private final FHIRPathEngine pathEngine;

  private FHIRPathEndpoint(FHIRPathEngine engine) {
    this.pathEngine = engine;
    createRoutes();
  }

  /**
   * Get the existing FHIRPathEndpoint or create one if it does not already exist.
   *
   * @param engine the FHIRPathEngine that should be used for this endpoint
   * @return the singleton FHIRPathEndpoint
   */
  public static FHIRPathEndpoint getInstance(FHIRPathEngine engine) {
    if (fhirPathEndpoint == null) {
      fhirPathEndpoint = new FHIRPathEndpoint(engine);
    }
    return fhirPathEndpoint;
  }

  /**
   * Creates the API routes for receiving and processing requests for the FHIRPath service.
   */
  private void createRoutes() {
  }
}
