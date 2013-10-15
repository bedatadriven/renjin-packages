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

	@OneToMany(mappedBy = "rPackage")
	private Set<RPackageVersion> dependencies;

	@Temporal(TemporalType.TIMESTAMP)
	private Date publicationDate;
  
  private boolean sourceDownloaded;
  
  
  @Lob
  private String description;

	@Embedded
	private LOC loc = new LOC();

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

	public Set<RPackageVersion> getDependencies() {
		return dependencies;
	}

	public void setDependencies(Set<RPackageVersion> dependencies) {
		this.dependencies = dependencies;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
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
}
