package org.mitre.inferno;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.FhirPublication;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.hl7.fhir.utilities.validation.ValidationMessage.IssueSeverity;
import org.hl7.fhir.validation.BaseValidator;
import org.hl7.fhir.validation.BaseValidator.ValidationControl;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.ValidationEngine.ValidationEngineBuilder;
import org.hl7.fhir.validation.cli.utils.VersionUtil;
import org.mitre.inferno.rest.IgResponse;

public class Validator {
  private final ValidationEngine hl7Validator;
  private final FilesystemPackageCacheManager packageManager;
  private final Map<String, NpmPackage> loadedPackages;

  /**
   * Creates the HL7 Validator to which can then be used for validation.
   *
   * @param igDir A directory containing tarred/gzipped IG packages
   * @throws Exception If the validator cannot be created
   */
  public Validator(String igDir) throws Exception {
    final String fhirSpecVersion = "4.0";
    final String definitions = VersionUtilities.packageForVersion(fhirSpecVersion)
        + "#" + VersionUtilities.getCurrentVersion(fhirSpecVersion);
    final String txServer = getTxServerUrl();
    final String txLog = null;
    final String fhirVersion = "4.0.1";

    ValidationEngineBuilder engineBuilder =
        new ValidationEngineBuilder().withTxServer(
                                                   txServer,
                                                   txLog,
                                                   FhirPublication.fromCode(fhirVersion)
                                                   );
    hl7Validator = engineBuilder.fromSource(definitions);
    
    // The two lines below turn off URL resolution checking in the validator. 
    // This eliminates the need to silence these errors elsewhere in Inferno
    // And also keeps contained resources from failing validation based solely on URL errors
    ValidationControl vc = new BaseValidator(null, null, false)
                             .new ValidationControl(false, IssueSeverity.INFORMATION);
    hl7Validator.getValidationControl().put("Type_Specific_Checks_DT_URL_Resolve", vc);

    // Get all the package gzips in the "igs/package" directory
    File dir = new File(igDir);
    File[] igFiles = dir.listFiles((d, name) -> name.endsWith(".tgz"));
    if (igFiles != null) {
      for (File igFile : igFiles) {
        hl7Validator
            .getIgLoader()
            .loadIg(
                    hl7Validator.getIgs(),
                    hl7Validator.getBinaries(),
                    igFile.getAbsolutePath(),
                    true
                    );
      }
    }

    hl7Validator.connectToTSServer(txServer, txLog, FhirPublication.fromCode(fhirVersion));
    hl7Validator.setDoNative(false);
    hl7Validator.setAnyExtensionsAllowed(true);
    hl7Validator.prepare();

    packageManager = new FilesystemPackageCacheManager(true);
    loadedPackages = new HashMap<>();
  }

  /**
   * Lists the names of resources defined for this version of the validator.
   *
   * @return a sorted list of distinct resource names
   */
  public List<String> getResources() {
    return hl7Validator.getContext().getResourceNames()
        .stream()
        .sorted()
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Lists the StructureDefinitions loaded in the validator.
   *
   * @return a sorted list of distinct structure canonicals
   */
  public List<String> getStructures() {
    List<StructureDefinition> structures =
        hl7Validator
            .getContext()
            .fetchResourcesByType(StructureDefinition.class);
    return structures
        .stream()
        .map(StructureDefinition::getUrl)
        .sorted()
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Validates the given resource against the given list of profiles.
   *
   * @param resource a byte array representation of a FHIR resource
   * @param profiles a list of profile URLs to validate against
   * @return an OperationOutcome resource representing the result of the validation operation
   */
  public OperationOutcome validate(byte[] resource, List<String> profiles) {
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(resource);
    ByteArrayInputStream resourceStream = new ByteArrayInputStream(resource);
    OperationOutcome oo;
    try {
      oo = hl7Validator.validate(fmt, resourceStream, profiles);
    } catch (Exception e) {
      // Add our own OperationOutcome for errors that break the ValidationEngine
      OperationOutcome.IssueSeverity sev = OperationOutcome.IssueSeverity.FATAL;
      OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent(
                                                                                sev,
                                                                                IssueType.STRUCTURE
                                                                                );
      issue.setDiagnostics(e.getMessage());
      issue.setDetails(new CodeableConcept().setText(e.getMessage()));
      issue.addExtension(
                         "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-line",
                         new IntegerType(1)
                         );
      issue.addExtension(
                         "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-col",
                         new IntegerType(1)
                         );
      issue.addExtension(
                         "http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-source",
                         new CodeType("ValidationService")
                         );
      oo = new OperationOutcome(issue);
    }
    return oo;
  }

  /**
   * Provides a map of known IGs that can be retrieved and loaded.
   *
   * @return a map containing each known IG ID and its corresponding canonical URL.
   */
  public Map<String, String> getKnownIGs() throws IOException {
    Map<String, String> igs = new HashMap<>();
    // Add known custom IGs
    for (Map.Entry<String, NpmPackage> e : loadedPackages.entrySet()) {
      String id = e.getKey().split("#")[0];
      String canonical = e.getValue().canonical();
      igs.put(id, canonical);
    }
    // Add IGs known to the package manager, replacing any conflicting package IDs
    packageManager.listAllIds(igs);
    return igs;
  }

  /**
   * Load a profile into the validator.
   *
   * @param profile the profile to be loaded
   */
  public void loadProfile(byte[] profile) throws IOException {
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(profile);
    Resource resource = FormatUtilities.makeParser(fmt).parse(profile);
    hl7Validator.getContext().cacheResource(resource);
  }

  /**
   * Finds any custom package that fits the given id and (possibly null) version.
   *
   * @param id the ID of the custom package
   * @param version the version of the custom package, or null to return the first match
   * @return a matching custom IG package, or null if no matching package was found
   */
  private NpmPackage findCustomPackage(String id, String version) {
    String idRegex = "^" + id + "#" + (version != null ? version : ".*") + "$";
    for (Map.Entry<String, NpmPackage> entry : loadedPackages.entrySet()) {
      if (entry.getKey().matches(idRegex)) {
        return entry.getValue();
      }
    }
    return null;
  }

  private IgResponse getIg(String id, String version) throws IOException {
    NpmPackage npm = findCustomPackage(id, version);
    // Fallback to packages from packages.fhir.org if no custom packages match
    if (npm == null) {
      npm = packageManager.loadPackage(id, version);
    }
    return IgResponse.fromPackage(npm);
  }

  /**
   * Load an IG into the validator.
   *
   * @param id the package ID of the FHIR IG to be loaded
   * @param version the package version of the FHIR IG to be loaded
   * @return an IgResponse representing the package that was loaded
   */
  public IgResponse loadIg(String id, String version) throws Exception {
    NpmPackage npm = findCustomPackage(id, version);
    // Fallback to packages from packages.fhir.org if no custom packages match
    if (npm == null) {
      hl7Validator
          .getIgLoader()
          .loadIg(
                  hl7Validator.getIgs(),
                  hl7Validator.getBinaries(),
                  id + (version != null ? "#" + version : ""),
                  true
                  );
      npm = packageManager.loadPackage(id, version);
    }
    return IgResponse.fromPackage(npm);
  }

  /**
   * Load a Gzipped IG into the validator.
   *
   * @param content the Gzip-encoded contents of the IG package to be loaded
   * @return an IgResponse representing the package that was loaded
   */
  public IgResponse loadPackage(byte[] content) throws Exception {
    File temp = File.createTempFile("package", ".tgz");
    temp.deleteOnExit();
    try {
      FileUtils.writeByteArrayToFile(temp, content);
      hl7Validator
          .getIgLoader()
          .loadIg(
                  hl7Validator.getIgs(),
                  hl7Validator.getBinaries(),
                  temp.getCanonicalPath(),
                  true
                  );
    } finally {
      temp.delete();
    }
    NpmPackage npm = NpmPackage.fromPackage(new ByteArrayInputStream(content));
    loadedPackages.put(npm.id() + "#" + npm.version(), npm);
    return IgResponse.fromPackage(npm);
  }

  /**
   * Get a mapping from IG URL to a list of profile URLs supported by the IG.
   *
   * @return a mapping from IG URL to a list of profile URLs supported by the IG.
   */
  public Map<String, List<String>> getProfilesByIg() {
    List<ImplementationGuide> igs = hl7Validator.getContext().allImplementationGuides();
    return igs
        .stream()
        .collect(Collectors.toMap(
            ImplementationGuide::getPackageId,
            ig -> {
              try {
                return getIg(ig.getPackageId(), ig.getVersion()).getProfiles();
              } catch (IOException e) {
                return new ArrayList<>();
              }
            },
            (existing, replacement) -> existing
        ));
  }

  public String getVersion() {
    return VersionUtil.getVersion();
  }

  /**
   * Load a profile from a file.
   *
   * @param src the file path
   * @throws IOException if the file fails to load
   */
  public void loadProfileFromFile(String src) throws IOException {
    byte[] profile = loadResourceFromFile(src);
    loadProfile(profile);
  }

  private byte[] loadResourceFromFile(String src) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    URL file = classLoader.getResource(src);
    return IOUtils.toByteArray(file);
  }

  private String getTxServerUrl() {
    if (disableTxValidation()) {
      return null;
    }

    if (System.getenv("TX_SERVER_URL") != null) {
      return System.getenv("TX_SERVER_URL");
    } else {
      return "http://tx.fhir.org";
    }
  }

  private boolean disableTxValidation() {
    return System.getenv("DISABLE_TX") != null;
  }
}
