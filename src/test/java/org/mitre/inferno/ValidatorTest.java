package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.mitre.inferno.rest.IgResponse;

public class ValidatorTest {
  private static Validator validator;

  @BeforeEach
  public void setUp() throws Exception {
    validator = new Validator("./igs", true);
  }

  @AfterEach
  public void cleanUp() throws Exception {
    validator = null;
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
      String profileUrl = "http://hl7.org/fhir/StructureDefinition/blah";
      boolean condition = isProfileLoaded(profileUrl);
      assertFalse(condition);
      validator.loadProfile(profile);
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
  @DisabledIfEnvironmentVariable(named = "DISABLE_TX", matches = ".*")
  // This test is disabled when we don't use a terminology server to check codes.
  // Run it locally by unsetting the test environment var in build.gradle (or in your IDE of choice)
  void validateDisplayWarnings() {
    try {
      byte[] correct = loadFile("resource_with_correct_code_display.json");
      byte[] incorrect = loadFile("resource_with_incorrect_code_display.json");
      List<String> medicationProfile = Arrays.asList("http://hl7.org/fhir/StructureDefinition/Medication");

      // first set display issues = errors, the default
      validator = new Validator("./igs", false);

      OperationOutcome oo = validator.validate(correct, medicationProfile);
      assertHasValidationErrors(oo, false); // no errors for the correct display

      oo = validator.validate(incorrect, medicationProfile);
      assertHasValidationErrors(oo, true); // an error expected for the incorrect display

      // now set display issues = warnings
      validator = new Validator("./igs", true);

      oo = validator.validate(correct, medicationProfile);
      assertHasValidationErrors(oo, false); // still no errors for the correct display

      oo = validator.validate(incorrect, medicationProfile);
      assertHasValidationErrors(oo, false); // no errors anymore for the incorrect display
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  void assertHasValidationErrors(OperationOutcome oo, boolean areErrorsExpected) {
    String codesForErrors = oo.getIssue()
        .stream()
        .filter(i -> i.getSeverity() == IssueSeverity.ERROR
                  || i.getSeverity() == IssueSeverity.FATAL)
        .map(i -> i.getDetails().getText())
        .collect(Collectors.joining(","));

    if (areErrorsExpected) {
      assertFalse(codesForErrors.isBlank(), codesForErrors);
    } else {
      assertTrue(codesForErrors.isBlank(), codesForErrors);
    }
  }

  @Test
  void getKnownIGs() throws IOException {
    Set<String> knownIGs = validator.getKnownIGs().keySet();
    assertTrue(knownIGs.contains("hl7.fhir.r4.core") || knownIGs.contains("hl7.fhir.us.core"));
  }

  @Test
  void loadIg() throws Exception {
    // A small subset of the profiles in mCODE 3.0.0
    List<String> profilesToLoad = Arrays.asList(
        "http://hl7.org/fhir/us/mcode/StructureDefinition/mcode-cancer-disease-status",
        "http://hl7.org/fhir/us/mcode/StructureDefinition/mcode-cancer-patient",
        "http://hl7.org/fhir/us/mcode/StructureDefinition/mcode-cancer-related-medication-request",
        "http://hl7.org/fhir/us/mcode/StructureDefinition/mcode-cancer-related-surgical-procedure",
        "http://hl7.org/fhir/us/mcode/StructureDefinition/mcode-cancer-stage-group"
    );
    assertTrue(profilesToLoad.stream().noneMatch(this::isProfileLoaded));

    // Because the version isn't given, this should load the "current" version of hl7.fhir.us.mcode
    // Note this means that if a new version is published that changes the profile names
    // (as has happened in updates 1->2 and 2->3) this test may fail
    IgResponse ig = validator.loadIg("hl7.fhir.us.mcode", null);
    assertEquals("hl7.fhir.us.mcode", ig.id);
    assertTrue(ig.profiles.containsAll(profilesToLoad));
    assertTrue(profilesToLoad.stream().allMatch(this::isProfileLoaded));
  }

  @Test
  void loadIgWithVersions() throws Exception {
    // A subset of the 45 profiles in hl7.fhir.us.qicore#3.3.0
    List<String> oldProfilesToLoad = Arrays.asList(
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medication",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationadministration",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationdispense",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationrequest",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationstatement",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-military-service",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-nutritionorder",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-observation",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-organization",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-practitioner",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-practitionerrole",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-procedure"
    );
    assertTrue(oldProfilesToLoad.stream().noneMatch(this::isProfileLoaded));

    IgResponse ig = validator.loadIg("hl7.fhir.us.qicore", "3.3.0");
    assertEquals("hl7.fhir.us.qicore", ig.id);
    assertEquals("3.3.0", ig.version);
    assertEquals(45, ig.profiles.size());

    // All old profiles to load have been loaded and are returned in the resulting list
    assertTrue(ig.profiles.containsAll(oldProfilesToLoad));
    assertTrue(oldProfilesToLoad.stream().allMatch(this::isProfileLoaded));

    // All of the profiles that are in hl7.fhir.us.qicore#4.9.0, but not hl7.fhir.us.qicore#3.3.0
    List<String> newProfilesToLoad = Arrays.asList(
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-communicationnotdone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-devicenotrequested",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-doNotPerformReason",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-encounter-diagnosisPresentOnAdmission",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-immunizationnotdone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-isElective",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationnotdispensed",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-medicationnotrequested",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-mednotadministered",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-notDoneReason",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-observationnotdone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-procedurenotdone",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-recorded",
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-servicenotrequested"
    );
    assertTrue(newProfilesToLoad.stream().noneMatch(this::isProfileLoaded));

    // There are 15 added profiles and 2 removed profiles going from version 3.3.0 to 4.9.0
    ig = validator.loadIg("hl7.fhir.us.qicore", "4.9.0");
    assertEquals("hl7.fhir.us.qicore", ig.id);
    assertEquals("4.9.0", ig.version);
    assertEquals(58, ig.profiles.size());

    // All new profiles to load have been loaded and are returned in the resulting list
    assertTrue(ig.profiles.containsAll(newProfilesToLoad));
    assertTrue(newProfilesToLoad.stream().allMatch(this::isProfileLoaded));
  }

  @Test
  void loadPackage() throws Exception {
    final List<String> profilesToLoad = Arrays.asList(
        "http://hl7.org.au/fhir/StructureDefinition/au-address",
        "http://hl7.org.au/fhir/StructureDefinition/au-assigningauthority",
        "http://hl7.org.au/fhir/StructureDefinition/au-healthcareservice",
        "http://hl7.org.au/fhir/StructureDefinition/au-location",
        "http://hl7.org.au/fhir/StructureDefinition/au-organization",
        "http://hl7.org.au/fhir/StructureDefinition/au-practitioner",
        "http://hl7.org.au/fhir/StructureDefinition/au-practitionerrole",
        "http://hl7.org.au/fhir/StructureDefinition/au-receivingapplication",
        "http://hl7.org.au/fhir/StructureDefinition/au-receivingfacility",
        "http://hl7.org.au/fhir/StructureDefinition/encryption-certificate-pem-x509",
        "http://hl7.org.au/fhir/StructureDefinition/no-fixed-address"
    );
    assertTrue(profilesToLoad.stream().noneMatch(this::isProfileLoaded));
    IgResponse ig = validator.loadPackage(loadFile("hl7.fhir.au.base.tgz"));
    assertEquals("hl7.fhir.au.base", ig.id);
    ig.profiles.containsAll(profilesToLoad);
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
