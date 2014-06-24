package org.renjin.build.model;

import com.google.common.collect.Sets;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;

import javax.persistence.Transient;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Records metadata about an individual package
 * build attempt
 */
@Entity
public class PackageBuild {

  @Id
  private String id;

  private BuildOutcome outcome;

  /**
   * The Renjin version against which the
   * package was built
   */
  private String renjinVersion;

  @Index
  private BuildStage stage;

  @Unindex
  private List<String> dependencyVersions;

  @Index
  private List<String> blockingDependencies;

  /**
   * The id of the worker that leased this build
   */
  private String workerId;

  /**
   * The time at which this RPackageBuild was
   * leased by a worker. Should be null if the stage is COMPLETE.
   */
  @Index
  private Long leaseTime;

  /**
   * Time at which this PackageBuild completed, whether
   * successful or not
   */
  private long completionTime;


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Transient
  public boolean isSucceeded() {
    return outcome == BuildOutcome.SUCCESS;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  @Transient
  public String getPackageName() {
    String[] parts = id.split(":");
    return parts[1];
  }

  public BuildStage getStage() {
    return stage;
  }

  public void setStage(BuildStage stage) {
    this.stage = stage;
    switch(stage) {
      case WAITING:
      case READY:
        leaseTime = 0L;
        break;
      case LEASED:
        leaseTime = System.currentTimeMillis();
        break;
      case COMPLETED:
        leaseTime = null;
        break;
    }
  }

  public List<String> getDependencyVersions() {
    return dependencyVersions;
  }

  public void setDependencyVersions(List<String> dependencyVersions) {
    this.dependencyVersions = dependencyVersions;
  }

  public String getWorkerId() {
    return workerId;
  }

  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  public long getLeaseTime() {
    return leaseTime;
  }

  public long getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(long completionTime) {
    this.completionTime = completionTime;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public List<String> getBlockingDependencies() {
    return blockingDependencies;
  }

  public void setBlockingDependencies(List<String> blockingDependencies) {
    this.blockingDependencies = blockingDependencies;
  }

  public void setLeaseTime(Long leaseTime) {
    this.leaseTime = leaseTime;
  }

  public boolean isComplete() {
    return stage == BuildStage.COMPLETED;
  }

  public String getPackageVersionId() {
    String[] parts = id.split(":");
    return parts[0] + ":" + parts[1] + ":" + parts[2];
  }
}
