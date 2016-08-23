package org.renjin.ci.jenkins.tools;

import org.renjin.ci.model.RenjinVersionId;

/**
 * Tests for capabilities of a given version of Renjin
 */
public class RenjinCapabilities {
  
  public static boolean hasMake(RenjinVersionId renjinVersionId) {
    return renjinVersionId.compareTo(new RenjinVersionId("0.8.2037")) >= 0;
  }
  
  public static boolean hasCompiler(RenjinVersionId renjinVersionId) {
    return renjinVersionId.compareTo(new RenjinVersionId("0.8.2063")) >= 0;
  }
  
  public static boolean hasUnpackJars(RenjinVersionId renjinVersionId) {
    return renjinVersionId.compareTo(new RenjinVersionId("0.8.2194")) >= 0;
  }
}
