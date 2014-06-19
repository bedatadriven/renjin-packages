package org.renjin.build.model;


import com.google.common.collect.Sets;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RPackageBuild {

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

  private boolean nativeSourceCompilationFailures;

  private Integer delta;

  @Column(nullable = false, columnDefinition = "tinyint")
  private boolean dependenciesResolved;

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

  @Enumerated(EnumType.STRING)
  @Column(name="outcome_type")
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

  public boolean isDependenciesResolved() {
    return dependenciesResolved;
  }

  public void setDependenciesResolved(boolean dependenciesResolved) {
    this.dependenciesResolved = dependenciesResolved;
  }
}
