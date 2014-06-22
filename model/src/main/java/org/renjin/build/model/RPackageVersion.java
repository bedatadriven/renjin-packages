package org.renjin.build.model;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RPackageVersion implements Comparable<RPackageVersion> {

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
  private Set<RPackageBuild> buildResults;

  @OneToMany(mappedBy = "packageVersion", cascade = CascadeType.ALL)
  private Set<RPackageDependency> dependencies;

  @OneToMany(mappedBy = "dependency")
  private Set<RPackageDependency> reverseDependencies;

  private int downstreamCount;

  @Column(nullable = false, columnDefinition = "tinyint")
  private boolean dependenciesResolved;

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

  @Transient
  public ArtifactVersion getArtifactVersion() {
    return new DefaultArtifactVersion(getVersion());
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

  public Set<RPackageBuild> getBuildResults() {
    return buildResults;
  }

  public void setBuildResults(Set<RPackageBuild> buildResults) {
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

  public int getDownstreamCount() {
    return downstreamCount;
  }

  public void setDownstreamCount(int downstreamCount) {
    this.downstreamCount = downstreamCount;
  }

  public boolean isDependenciesResolved() {
    return dependenciesResolved;
  }

  public void setDependenciesResolved(boolean dependenciesResolved) {
    this.dependenciesResolved = dependenciesResolved;
  }

  @Override
  public String toString() {
    return id;
  }

  @Override
  public int compareTo(@Nonnull RPackageVersion o) {
    if(this.getPackage().getId().equals(o.getPackage().getId())) {
      // if these are two versions of the same package, compare by version number
      return this.getArtifactVersion().compareTo(o.getArtifactVersion());
    } else {
      // otherwise compare by package name
      return this.getId().compareTo(o.getId());
    }
  }
}
