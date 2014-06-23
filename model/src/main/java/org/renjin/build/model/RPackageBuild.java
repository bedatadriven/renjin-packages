package org.renjin.build.model;


import com.google.common.collect.Sets;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RPackageBuild {

	@Id
	private String id;

  @Enumerated(EnumType.STRING)
	private BuildOutcome outcome;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, columnDefinition = "varchar(50) default 'COMPLETED'")
  private BuildStage stage;

	@ManyToOne
	private Build build;

	@ManyToOne
	private RPackageVersion packageVersion;

  @Transient
  private Set<TestResult> testResults = Sets.newHashSet();

  @Lob
  private String dependencyVersions;

  private boolean nativeSourceCompilationFailures;

  /**
   * True if this worker
   */
  private String leased;

  /**
   * The time at which this RPackageBuild was
   * leased by a worker
   */
  @Temporal(TemporalType.TIMESTAMP)
  private Date leaseTime;


  @Temporal(TemporalType.TIMESTAMP)
  private Date completionTime;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Transient
  public String getPath() {
    return getBuild().getId() + "/" + getPackageVersion().getPath();
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

	public Build getBuild() {
		return build;
	}

	public void setBuild(Build build) {
		this.build = build;
	}

	public RPackageVersion getPackageVersion() {
		return packageVersion;
	}

  @Transient
  public RPackage getPackage() {
    return packageVersion.getPackage();
  }

  @Transient
  public String getPackageName() {
    String[] parts = packageVersion.getId().split(":");
    return parts[1];
  }

	public void setPackageVersion(RPackageVersion packageVersion) {
		this.packageVersion = packageVersion;
	}

  public boolean isNativeSourceCompilationFailures() {
    return nativeSourceCompilationFailures;
  }

  public void setNativeSourceCompilationFailures(boolean nativeSourceCompilationFailures) {
    this.nativeSourceCompilationFailures = nativeSourceCompilationFailures;
  }

  public Set<TestResult> getTestResults() {
    return testResults;
  }

  public void setTestResults(Set<TestResult> testResults) {
    this.testResults = testResults;
  }

  @Column(nullable = true)
  public String getLeased() {
    return leased;
  }

  public void setLeased(String leased) {
    this.leased = leased;
  }

  @Column(nullable = true)
  public Date getLeaseTime() {
    return leaseTime;
  }

  public void setLeaseTime(Date leaseTime) {
    this.leaseTime = leaseTime;
  }

  public BuildStage getStage() {
    return stage;
  }

  public void setStage(BuildStage stage) {
    this.stage = stage;
  }

  public Date getCompletionTime() {
    return completionTime;
  }

  public void setCompletionTime(Date completionTime) {
    this.completionTime = completionTime;
  }

  public String getDependencyVersions() {
    return dependencyVersions;
  }

  public void setDependencyVersions(String dependencyVersions) {
    this.dependencyVersions = dependencyVersions;
  }
}
