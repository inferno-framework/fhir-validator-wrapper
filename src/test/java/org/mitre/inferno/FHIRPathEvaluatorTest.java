package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FHIRPathEvaluatorTest {
  private static FHIRPathEvaluator pathEvaluator;

  @BeforeAll
  static void setUp() throws Exception {
    pathEvaluator = new FHIRPathEvaluator();
  }

  @Test
  void evaluateToString() throws IOException {
    Resource patient = loadResource("patient_fixture.json");
    assertEquals("[]", pathEvaluator.evaluateToString(patient, "Patient.foo"));
    assertEquals(
        "[{\"type\":\"string\",\"element\":\"234\"}]",
        pathEvaluator.evaluateToString(patient, "Patient.id.substring(1,3)")
    );
    assertEquals(
        "["
            + "{\"type\":\"string\",\"element\":\"A\"},"
            + "{\"type\":\"string\",\"element\":\"B\"},"
            + "{\"type\":\"string\",\"element\":\"C\"}"
            + "]",
        pathEvaluator.evaluateToString(patient, "Patient.name.given")
    );
    assertEquals(
        "["
            + "{\"type\":\"HumanName\",\"element\":{\"given\":[\"A\"]}},"
            + "{\"type\":\"HumanName\",\"element\":{\"given\":[\"B\"]}},"
            + "{\"type\":\"HumanName\",\"element\":{\"given\":[\"C\"]}}"
            + "]",
        pathEvaluator.evaluateToString(patient, "Patient.name")
    );
    assertEquals(
        "["
          + "{"
            + "\"type\":\"Patient.communication\","
              + "\"element\":{"
                + "\"language\":{"
                  + "\"coding\":["
                    + "{"
                      + "\"system\":\"urn:ietf:bcp:47\","
                      + "\"code\":\"en-US\","
                      + "\"display\":\"English (United States)\""
                    + "}"
                  + "]"
                + "},"
              + "\"preferred\":true"
            + "}"
          + "}"
        + "]",
        pathEvaluator.evaluateToString(patient, "Patient.communication.where(preferred = true)")
    );
  }

  private Resource loadResource(String filename) throws IOException {
    return new JsonParser().parse(loadFile(filename));
  }

  byte[] loadFile(String fileName) throws IOException {
    return IOUtils.toByteArray(getClass().getClassLoader().getResource(fileName));
  }
}
