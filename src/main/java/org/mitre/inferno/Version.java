package org.mitre.inferno;

import java.io.IOException;
import java.util.Properties;

public class Version {
  private static final String version;

  static {
    String temp_version;
    try {
      Properties properties = new Properties();
      properties.load(Version.class.getClassLoader().getResourceAsStream("version.properties"));
      temp_version = properties.getProperty("version");;
    } catch (IOException e) {
      temp_version = null;
    }
    version = temp_version;
  }

  public static String getVersion() {
    return version;
  }
}
