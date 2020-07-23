package org.mitre.inferno.rest;

import static spark.Spark.post;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Resource;
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
    post("/evaluate", (req, res) -> {
      res.type("application/fhir+json");
      return evaluate(req.bodyAsBytes(), req.queryParams("path"));
    });
  }

  private String convertToString(Base item) {
    String type = item.fhirType();
    try {
      if (item.isResource()) {
        return new JsonParser().composeString((Resource) item);
      } else if (item instanceof DataType) {
        return new JsonParser().composeString((DataType) item, type);
      }
    } catch (IOException e) {
      throw new FHIRException(String.format("Failed to compose item of type [%s].", type), e);
    }
    throw new IllegalArgumentException(String.format("[%s] was not a Resource or DataType.", type));
  }

  private String evaluate(byte[] body, String path) throws IOException {
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(body);
    Resource rootResource = FormatUtilities.makeParser(fmt).parse(body);
    List<Base> result = pathEngine.evaluate(rootResource, path);
    return '['
        + result.stream().map(this::convertToString).collect(Collectors.joining(","))
        + ']';
  }
}
