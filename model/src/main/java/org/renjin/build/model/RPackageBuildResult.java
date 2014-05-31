package org.renjin.build.model;


import com.google.common.collect.Sets;

import javax.persistence.*;
import java.util.Set;

@Entity
public class RPackageBuildResult {

	@Id
  @GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	private BuildOutcome outcome;

	@ManyToOne
	private Build build;

	@ManyToOne
	private RPackageVersion packageVersion;

  @Transient
  private Set<TestResult> testResults = Sets.newHashSet();

  @Column(nullable = false)
  private boolean latest;

  private boolean testFailures;

  private boolean nativeSourceCompilationFailures;

  private Integer delta;

  private boolean succeeded;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

  @Transient
  public String getPath() {
    return getBuild().getId() + "/" + getPackageVersion().getPath();
  }

	public BuildOutcome getOutcome() {
		return outcome;
	}

	public void setOutcome(BuildOutcome outcome) {
		this.outcome = outcome;
    this.succeeded = (outcome == BuildOutcome.SUCCESS);
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

	public void setPackageVersion(RPackageVersion packageVersion) {
		this.packageVersion = packageVersion;
	}

  public boolean isTestFailures() {
    return testFailures;
  }

  public void setTestFailures(boolean testFailures) {
    this.testFailures = testFailures;
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

  public Integer getDelta() {
    return delta;
  }

  public void setDelta(Integer delta) {
    this.delta = delta;
  }

  public boolean isSucceeded() {
    return succeeded;
  }

  public void setSucceeded(boolean succeeded) {
    this.succeeded = succeeded;
  }
}