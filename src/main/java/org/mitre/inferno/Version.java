package org.mitre.inferno;

import java.io.IOException;
import java.util.Properties;
import org.slf4j.LoggerFactory;

public class Version {
  private static final String version;

  static {
    String tempVersion;
    try {
      Properties properties = new Properties();
      properties.load(Version.class.getClassLoader().getResourceAsStream("version.properties"));
      tempVersion = properties.getProperty("version");
    } catch (IOException e) {
      tempVersion = null;
      LoggerFactory.getLogger(Version.class).error("Failed to retrieve version: " + e.getMessage());
    }
    version = tempVersion;
  }

  public static String getVersion() {
    return version;
  }
}
