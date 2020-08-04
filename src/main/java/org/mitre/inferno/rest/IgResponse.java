package org.mitre.inferno.rest;

import java.util.List;

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
