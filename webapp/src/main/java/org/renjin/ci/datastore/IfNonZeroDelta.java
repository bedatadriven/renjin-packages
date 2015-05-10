package org.renjin.ci.datastore;

import com.googlecode.objectify.condition.PojoIf;

/**
 * Predicate which returns true if the {@code PackageBuild} has a non-zero delta
 */
public class IfNonZeroDelta extends PojoIf<PackageBuild> {
  @Override
  public boolean matchesPojo(PackageBuild build) {
    return build.getBuildDelta() != 0 || build.getCompilationDelta() != 0;
  }
}
