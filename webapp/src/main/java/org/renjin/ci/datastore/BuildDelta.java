package org.renjin.ci.datastore;

import com.google.common.base.Optional;
import org.renjin.ci.model.RenjinVersionId;

import java.util.HashSet;
import java.util.Set;

/**
 * Describes a build's change in status with regard to building, native compilation,
 * and test results
 */
public class BuildDelta {
  
  private long buildNumber;
  private String renjinVersion;
  private int buildDelta = 0;
  private int compilationDelta = 0;
  
  private long lastSuccessfulBuild;
  private String lastSuccessfulRenjinVersion;
  
  private Set<String> testRegressions = new HashSet<>();
  private Set<String> testProgressions = new HashSet<>();

  public BuildDelta() {
  }

  public BuildDelta(PackageBuild build) {
    buildNumber = build.getBuildNumber();
    renjinVersion = build.getRenjinVersion();
  }

  public BuildDelta(long buildNumber, RenjinVersionId renjinVersionId) {
    this.buildNumber = buildNumber;
    this.renjinVersion = renjinVersionId.toString();
  }

  public long getBuildNumber() {
    return buildNumber;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public Set<String> getTestRegressions() {
    return testRegressions;
  }

  public Set<String> getTestProgressions() {
    return testProgressions;
  }

  public int getBuildDelta() {
    return buildDelta;
  }

  public void setBuildDelta(int buildDelta) {
    this.buildDelta = buildDelta;
  }

  public int getCompilationDelta() {
    return compilationDelta;
  }

  public void setCompilationDelta(int compilationDelta) {
    this.compilationDelta = compilationDelta;
  }

  public RenjinVersionId getRenjinVersionId() {
    return RenjinVersionId.valueOf(renjinVersion);
  }

  public long getLastSuccessfulBuild() {
    return lastSuccessfulBuild;
  }

  public void setLastSuccessfulBuild(long lastSuccessfulBuild) {
    this.lastSuccessfulBuild = lastSuccessfulBuild;
  }

  public String getLastSuccessfulRenjinVersion() {
    return lastSuccessfulRenjinVersion;
  }

  public void setLastSuccessfulRenjinVersion(String lastSuccessfulRenjinVersion) {
    this.lastSuccessfulRenjinVersion = lastSuccessfulRenjinVersion;
  }

  public Optional<RenjinVersionId> getLastSuccessfulRenjinVersionId() {
    if(lastSuccessfulRenjinVersion == null) {
      return Optional.absent();
    } else {
      return Optional.of(RenjinVersionId.valueOf(lastSuccessfulRenjinVersion));
    }
  }
}
