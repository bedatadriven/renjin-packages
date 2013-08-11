package org.renjin.repo.model;


import javax.persistence.*;

@Entity
public class RPackageBuildResult {

	@Id
<<<<<<< HEAD
  @GeneratedValue(strategy = GenerationType.AUTO)
=======
        @GeneratedValue(strategy = GenerationType.AUTO)
>>>>>>> fd0a5e157685cf791af9d239b31cd8576823602d
	private int id;

	@Lob
	private String log;

	private BuildOutcome outcome;

	@ManyToOne
	private Build build;

	@ManyToOne
	private RPackageVersion packageVersion;

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
}
