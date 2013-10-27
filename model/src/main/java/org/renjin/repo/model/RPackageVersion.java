package org.renjin.repo.model;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RPackageVersion {

	/**
	 * Package id in the form groupId:artifactId:version (e.g. org.renjin.cran:lattice:0.16)
	 */
	@Id
	private String id;

	@ManyToOne
	private RPackage rPackage;

	private String version;

	@Temporal(TemporalType.TIMESTAMP)
	private Date publicationDate;
  
  private boolean sourceDownloaded;

  @Lob
  private String description;

	@Embedded
	private LOC loc = new LOC();

  @Column(nullable = false)
  private boolean latest;

  @OneToMany(mappedBy = "packageVersion")
  private Set<RPackageBuildResult> buildResults;

  @OneToMany(mappedBy = "packageVersion", cascade = CascadeType.ALL)
  private Set<RPackageDependency> dependencies;

  @OneToMany(mappedBy = "dependency")
  private Set<RPackageDependency> reverseDependencies;

  /**
   * The version of GNU R on which this package depends
   */
  private String gnuRDependency;

  public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
  
  @Transient
  public String getPackageName() {
    String gav[] = id.split(":");
    return gav[1];
  }

  @Transient
  public String getGroupId() {
    String gav[] = id.split(":");
    return gav[0];
  }

  @Transient
  public RPackage getPackage() {
    return rPackage;
  }

  @Transient
  public String getPath() {
    return getGroupId() + "/" + getPackageName() + "/" + getVersion();
  }

	public RPackage getRPackage() {
		return rPackage;
	}

	public void setRPackage(RPackage rPackage) {
		this.rPackage = rPackage;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

  public Set<RPackageBuildResult> getBuildResults() {
    return buildResults;
  }

  public void setBuildResults(Set<RPackageBuildResult> buildResults) {
    this.buildResults = buildResults;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LOC getLoc() {
		return loc;
	}

	public void setLoc(LOC loc) {
		this.loc = loc;
	}

  public boolean isSourceDownloaded() {
    return sourceDownloaded;
  }

  public void setSourceDownloaded(boolean sourceDownloaded) {
    this.sourceDownloaded = sourceDownloaded;
  }

  public void setLatest(boolean latest) {
    this.latest = latest;
  }

  public boolean isLatest() {
    return latest;
  }

  public Set<RPackageDependency> getReverseDependencies() {
    return reverseDependencies;
  }

  public void setReverseDependencies(Set<RPackageDependency> reverseDependencies) {
    this.reverseDependencies = reverseDependencies;
  }

  public void setDependencies(Set<RPackageDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public Set<RPackageDependency> getDependencies() {
    return dependencies;
  }

  public String getGnuRDependency() {
    return gnuRDependency;
  }

  public void setGnuRDependency(String gnuRDependency) {
    this.gnuRDependency = gnuRDependency;
  }
}
