package org.mitre.inferno;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

public class FHIRPathEvaluator extends FHIRPathEngine {

  public FHIRPathEvaluator() throws IOException {
    super(new SimpleWorkerContext());
  }

  @Override
  public String evaluateToString(Base base, String path) {
    List<Base> result = this.evaluate(base, path);
    return '['
        + result.stream().map(this::baseToJson).collect(Collectors.joining(","))
        + ']';
  }

  private String baseToJson(Base item) {
    String repr = new JsonParser().composeString(item);
    return String.format("{\"type\":\"%s\",\"element\":%s}", item.fhirType(), repr);
  }
}
