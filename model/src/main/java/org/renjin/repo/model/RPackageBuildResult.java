package org.renjin.repo.model;


import javax.persistence.*;
import java.util.Set;

@Entity
public class RPackageBuildResult {

	@Id
  @GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Lob
	private String log;

	private BuildOutcome outcome;

	@ManyToOne
	private Build build;

	@ManyToOne
	private RPackageVersion packageVersion;

  @OneToMany(mappedBy = "buildResult")
  private Set<TestResult> testResults;

  private boolean testFailures;

  private boolean nativeSourceCompilationFailures;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
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
}
