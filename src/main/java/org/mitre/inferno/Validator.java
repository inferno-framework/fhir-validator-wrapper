package org.mitre.inferno;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.cache.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.cache.ToolsVersion;

public class Validator {
  ValidationEngine hl7Validator;
  FilesystemPackageCacheManager packageManager;

  /**
   * The Validator is capable of validating FHIR Resources against FHIR Profiles.
   *
   * @param igFile The igFile the validator is loaded with.
   */
  public Validator(String igFile) {
    try {
      this.createHL7Validator(igFile);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the PackageManager used by the validator
   *
   * <p>This can be used to retrieve the IGs which can be loaded.
   *
   * @return
   */
  /**
   * Creates the HL7 Validator to which can then be used for validation.
   *
   * @param igFile The name of the igFile to load
   * @throws Exception If the validator cannot be created
   */
  private void createHL7Validator(String igFile) throws Exception {
    final String fhirSpecVersion = "4.0";
    final String definitions = VersionUtilities.packageForVersion(fhirSpecVersion)
        + "#" + VersionUtilities.getCurrentVersion(fhirSpecVersion);
    final String txServer = getTxServerUrl();
    final String txLog = null;
    final String fhirVersion = "4.0.1";

    hl7Validator = new ValidationEngine(definitions);
    hl7Validator.loadIg(igFile, true);
    hl7Validator.connectToTSServer(txServer, txLog, FhirPublication.fromCode(fhirVersion));
    hl7Validator.setNative(false);
    hl7Validator.setAnyExtensionsAllowed(true);
    hl7Validator.prepare();

    packageManager = new FilesystemPackageCacheManager(true, ToolsVersion.TOOLS_VERSION);
  }

  public List<String> getResources() {
    return hl7Validator.getContext().getResourceNames();
  }

  /**
   * Lists the StructureDefinitions loaded in the validator.
   *
   * @return structures the list of structures
   */
  public List<String> getStructures() {
    List<StructureDefinition> structures = hl7Validator.getContext().getStructures();
    return structures
        .stream()
        .map(StructureDefinition::getUrl)
        .collect(Collectors.toList());
  }

  public OperationOutcome validate(byte[] resource, List<String> profiles) throws Exception {
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(resource);
    return this.hl7Validator.validate(null, resource, fmt, profiles);
  }

  /**
   * Provides a list of known IGs that can be retrieved and loaded.
   *
   * @return the list of IGs.
   */
  public List<String> getKnownIGs() {
    return packageManager.listPackages();
  }

  /**
   * Load a profile into the validator.
   * @param profile the profile to be loaded
   */
  public void loadProfile(Resource profile) {
    SimpleWorkerContext context = hl7Validator.getContext();
    context.cacheResource(profile);
  }

  /**
   * Load a profile from a file.
   *
   * @param src the file path
   * @throws IOException if the file fails to load
   */
  public void loadProfileFromFile(String src) throws IOException {
    byte[] resource = loadResourceFromFile(src);
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(resource);
    Resource profile = FormatUtilities.makeParser(fmt).parse(resource);
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
