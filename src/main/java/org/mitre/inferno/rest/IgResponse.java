package org.mitre.inferno.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.utilities.TextFile;
import org.hl7.fhir.utilities.json.JSONUtil;
import org.hl7.fhir.utilities.npm.NpmPackage;

public class IgResponse {
  public String id;
  public String version;
  public List<String> profiles;

  /**
   * Creates a new IgResponse, which represents the package info of an IG that was loaded into the
   * validator.
   * @param id the package id
   * @param version the package version
   * @param profiles the list of profile URLs belonging to this package
   */
  public IgResponse(String id, String version, List<String> profiles) {
    this.id = id;
    this.version = version;
    this.profiles = profiles;
  }

  /**
   * Creates an IgResponse representing the given NpmPackage.
   *
   * @param npm the NpmPackage to represent as an IgResponse
   * @return the IgResponse representing the given NpmPackage
   * @throws IOException if the package's .index.json could not be read
   */
  public static IgResponse fromPackage(NpmPackage npm) throws IOException {
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

  public String getId() {
    return id;
  }

  public String getVersion() {
    return version;
  }

  public List<String> getProfiles() {
    return profiles;
  }
}
