package org.mitre.inferno;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.FormatUtilities;
import org.hl7.fhir.r5.model.FhirPublication;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.cache.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.cache.NpmPackage;
import org.hl7.fhir.utilities.cache.ToolsVersion;
import org.hl7.fhir.utilities.json.JSONUtil;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.VersionUtil;
import org.mitre.inferno.rest.IgResponse;

public class Validator {
  private final ValidationEngine hl7Validator;
  private final FilesystemPackageCacheManager packageManager;
  private final Map<String, NpmPackage> loadedPackages;

  /**
   * Creates the HL7 Validator to which can then be used for validation.
   *
   * @param igFile The igFile the validator is loaded with.
   * @throws Exception If the validator cannot be created
   */
  public Validator(String igFile) throws Exception {
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
    List<StructureDefinition> structures = hl7Validator.getContext().getStructures();
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
   * @throws Exception if there was an error validating the resource
   */
  public OperationOutcome validate(byte[] resource, List<String> profiles) throws Exception {
    Manager.FhirFormat fmt = FormatUtilities.determineFormat(resource);
    return hl7Validator.validate(null, resource, fmt, profiles);
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
      igs.put(e.getKey(), e.getValue().canonical());
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

  private IgResponse getLoadedIg(String id, String version) throws IOException {
    NpmPackage npm = loadedPackages.get(id + "#" + version);
    if (npm == null) {
      npm = packageManager.loadPackage(id, version);
    }
    return getLoadedIg(npm);
  }

  private IgResponse getLoadedIg(NpmPackage npm) throws IOException {
    InputStream in = npm.load(".index.json");
    JsonObject index = (JsonObject) JsonParser.parseString(TextFile.streamToString(in));

    JsonArray files = index.getAsJsonArray("files");
    List<String> profileUrls = new ArrayList<>();
    for (JsonElement f : files) {
      JsonObject file = (JsonObject) f;
      String type = JSONUtil.str(file, "resourceType");
      String url = JSONUtil.str(file, "url");
      if (type.equals("StructureDefinition")) {
        profileUrls.add(url);
      }
    }
    Collections.sort(profileUrls);
    return new IgResponse(npm.id(), npm.version(), profileUrls);
  }

  /**
   * Load an IG into the validator.
   *
   * @param id the package ID of the FHIR IG to be loaded
   * @param version the package version of the FHIR IG to be loaded
   * @return an IgResponse representing the package that was loaded
   */
  public IgResponse loadIg(String id, String version) throws Exception {
    hl7Validator.loadIg(id + (version != null ? "#" + version : ""), true);
    return getLoadedIg(id, version);
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
      hl7Validator.loadIg(temp.getCanonicalPath(), true);
    } finally {
      temp.delete();
    }
    NpmPackage npm = NpmPackage.fromPackage(new ByteArrayInputStream(content));
    loadedPackages.put(npm.id() + "#" + npm.version(), npm);
    return getLoadedIg(npm);
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
                return getLoadedIg(ig.getPackageId(), ig.getVersion()).getProfiles();
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
