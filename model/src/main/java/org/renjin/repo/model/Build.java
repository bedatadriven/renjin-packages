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

  @ManyToOne
  @JoinColumn(name = "renjinCommitId")
	private RenjinCommit renjinCommit;

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

  public RenjinCommit getRenjinCommit() {
    return renjinCommit;
  }

  public void setRenjinCommit(RenjinCommit renjinCommit) {
    this.renjinCommit = renjinCommit;
  }

  public Set<RPackageBuildResult> getPackageResults() {
		return packageResults;
	}

	public void setPackageResults(Set<RPackageBuildResult> packageResults) {
		this.packageResults = packageResults;
	}
}
