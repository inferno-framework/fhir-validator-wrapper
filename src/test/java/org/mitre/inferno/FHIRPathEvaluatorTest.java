package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.DecimalType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StringType;
import org.hl7.fhir.r5.model.UriType;
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
    assertEquals("[\"234\"]", pathEvaluator.evaluateToString(patient, "Patient.id.substring(1,3)"));
    assertEquals("[\"A\",\"B\",\"C\"]", pathEvaluator.evaluateToString(patient, "Patient.name.given"));
    assertEquals(
        "["
            + "{\"given\":[\"A\"]},"
            + "{\"given\":[\"B\"]},"
            + "{\"given\":[\"C\"]}"
            + "]",
        pathEvaluator.evaluateToString(patient, "Patient.name")
    );
  }

  @Test
  void baseToString() {
    // Unquoted primitives
    assertEquals("false", pathEvaluator.baseToString(new BooleanType(false)));
    assertEquals("10", pathEvaluator.baseToString(new IntegerType(10)));
    assertEquals("3.51", pathEvaluator.baseToString(new DecimalType(3.51)));
    // Quoted primitives
    assertEquals("\"hello\"", pathEvaluator.baseToString(new StringType("hello")));
    assertEquals("\"foo\"", pathEvaluator.baseToString(new UriType("foo")));
  }

  private Resource loadResource(String filename) throws IOException {
    return new JsonParser().parse(loadFile(filename));
  }

  byte[] loadFile(String fileName) throws IOException {
    return IOUtils.toByteArray(getClass().getClassLoader().getResource(fileName));
  }
}
