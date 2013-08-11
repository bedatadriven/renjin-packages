package org.renjin.repo.model;


import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class Build {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	@Temporal(TemporalType.TIMESTAMP)
	private Date started;

	private String renjinVersion;

	private String renjinCommitId;

	@OneToMany(mappedBy = "build")
	private Set<RPackageBuildResult> packageResults;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getStarted() {
		return started;
	}

	public void setStarted(Date started) {
		this.started = started;
	}

	public String getRenjinVersion() {
		return renjinVersion;
	}

	public void setRenjinVersion(String renjinVersion) {
		this.renjinVersion = renjinVersion;
	}

	public String getRenjinCommitId() {
		return renjinCommitId;
	}

	public void setRenjinCommitId(String renjinCommitId) {
		this.renjinCommitId = renjinCommitId;
	}

	public Set<RPackageBuildResult> getPackageResults() {
		return packageResults;
	}

	public void setPackageResults(Set<RPackageBuildResult> packageResults) {
		this.packageResults = packageResults;
	}
}
