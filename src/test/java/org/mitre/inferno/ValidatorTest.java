package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class ValidatorTest {
  private static Validator validator;

  @BeforeAll
  static void setUp() throws Exception {
    validator = new Validator();
  }

  @Test
  void getStructures() {
    boolean condition = validator
        .getStructures()
        .contains("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient");
    assertTrue(condition);
  }

  @Test
  void getResources() {
    boolean condition = validator
        .getResources()
        .contains("Patient");
    assertTrue(condition);
  }

  @Test
  void loadProfile() {
    try {
      URL fixture = getClass()
          .getClassLoader()
          .getResource("profile_fixture.json");
      byte[] profile = IOUtils.toByteArray(fixture);
      Manager.FhirFormat fmt = FormatUtilities.determineFormat(profile);
      Resource resource = FormatUtilities.makeParser(fmt).parse(profile);
      String profileUrl = "http://hl7.org/fhir/StructureDefinition/blah";
      boolean condition = isProfileLoaded(profileUrl);
      assertFalse(condition);
      validator.loadProfile(resource);
      condition = isProfileLoaded(profileUrl);
      assertTrue(condition);
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  void loadProfileFromFile() {
    try {
      assertFalse(isProfileLoaded("http://hl7.org/fhir/StructureDefinition/foo"));
      validator.loadProfileFromFile("profile_file_fixture.json");
      assertTrue(isProfileLoaded("http://hl7.org/fhir/StructureDefinition/foo"));
    } catch (IOException e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  void validate() {
    try {
      byte[] example = loadFile("us_core_patient_example.json");
      validator.validate(example,
          Arrays.asList("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"));
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  void validateMultipleProfiles() {
    try {
      byte[] example = loadFile("us_core_patient_example.json");
      validator.validate(example, Arrays.asList("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient", "http://hl7.org/fhir/StructureDefinition/Patient"));
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  @Test
  void getKnownIGs() {
    List<String> knownIGs = validator.getKnownIGs();
    assertTrue(knownIGs.contains("hl7.fhir.r4.core#4.0.1"));
  }

  boolean isProfileLoaded(String profile) {
    return validator
        .getStructures()
        .contains(profile);
  }

  byte[] loadFile(String fileName) throws IOException {
    return IOUtils.toByteArray(getClass().getClassLoader().getResource(fileName));
  }
}
