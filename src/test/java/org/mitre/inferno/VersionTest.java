package org.mitre.inferno;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class VersionTest {

  @Test
  void versionNotNull() {
    assertNotNull(Version.getVersion());
  }

  @Test
  void versionFormattedCorrectly() {
    // Should match the regex: ^\d+\.\d+\.\d+$
    assertTrue(Version.getVersion().matches("^\\d+\\.\\d+\\.\\d+$"));
  }
}
