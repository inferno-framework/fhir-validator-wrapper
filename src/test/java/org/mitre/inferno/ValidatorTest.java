package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
    validator = new Validator("./igs/package");
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
  void getKnownIGs() throws IOException {
    Set<String> knownIGs = validator.getKnownIGs().keySet();
    assertTrue(knownIGs.contains("hl7.fhir.r4.core"));
  }

  @Test
  void loadIg() throws Exception {
    List<String> profilesToLoad = Arrays.asList(
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medication",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationadministration",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationdispense",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationnotdispensed",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationnotrequested",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationrequest",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationstatement",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-mednotadministered",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneReason",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-nutritionorder",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-observation",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-observationnotdone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-organization",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-practitioner",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-practitionerrole",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-procedure"
    );
    assertTrue(profilesToLoad.stream().noneMatch(this::isProfileLoaded));
    List<String> profileUrls = validator.loadIg("hl7.fhir.us.qicore");
    assertTrue(profileUrls.containsAll(profilesToLoad));
    assertTrue(profilesToLoad.stream().allMatch(this::isProfileLoaded));
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
