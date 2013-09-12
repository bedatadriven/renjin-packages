package org.renjin.repo.model;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
public class RPackageVersion {

	/**
	 * Package id in the form groupId:artifactId (e.g. org.renjin.cran:lattice)
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
