package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.junit.jupiter.api.Test;

class JsonParserTest {
  private static final JsonParser parser = new JsonParser();

  @Test
  void parse() throws IOException {
    // Full resources
    assertTrue(
        new Patient().equalsDeep(parser.parse("{\"resourceType\":\"Patient\"}", null))
    );

    // Complex datatypes
    assertTrue(
        new HumanName().addGiven("Foo")
            .equalsDeep(
                parser.parse("{\"given\":[\"Foo\"]}", "HumanName")
            )
    );

    // BackboneElements
    assertTrue(
        new Patient.PatientCommunicationComponent(
            new CodeableConcept().addCoding(
                new Coding()
                    .setSystem("urn:ietf:bcp:47")
                    .setCodeElement(new CodeType("en-US"))
                    .setDisplay("English (United States)")
            )
        ).equalsDeep(
            parser.parse(
                "{"
                    + "\"language\":"
                    + "{\"coding\":["
                    + "{\"system\":\"urn:ietf:bcp:47\","
                    + "\"code\":\"en-US\","
                    + "\"display\":\"English (United States)\""
                    + "}]}}",
                "Patient.communication"
            )
        )
    );
  }

  @Test
  void composeString() throws IOException {
    // Unquoted primitives
    assertEquals("false", parser.composeBase(new BooleanType(false)));
    assertEquals("10", parser.composeBase(new IntegerType(10)));
    assertEquals("3.51", parser.composeBase(new DecimalType(3.51)));
    // Quoted primitives
    assertEquals("\"hello\"", parser.composeBase(new StringType("hello")));
    assertEquals("\"foo\"", parser.composeBase(new UriType("foo")));

    // Full resources
    assertEquals("{\"resourceType\":\"Patient\"}", parser.composeBase(new Patient()));

    // Complex datatypes
    assertEquals("{\"given\":[\"Foo\"]}", parser.composeBase(new HumanName().addGiven("Foo")));

    // BackboneElements
    assertEquals(
        "{"
            + "\"language\":"
            + "{\"coding\":["
            + "{\"system\":\"urn:ietf:bcp:47\","
            + "\"code\":\"en-US\","
            + "\"display\":\"English (United States)\""
            + "}]}}",
        parser.composeBase(
            new Patient.PatientCommunicationComponent(
                new CodeableConcept().addCoding(
                    new Coding()
                        .setSystem("urn:ietf:bcp:47")
                        .setCodeElement(new CodeType("en-US"))
                        .setDisplay("English (United States)")
                )
            )
        )
    );
  }
}
