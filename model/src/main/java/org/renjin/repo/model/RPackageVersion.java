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

	@Embedded
	private LOC loc = new LOC();

  @OneToMany(mappedBy = "packageVersion")
  private Set<Test> tests;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

  public Set<Test> getTests() {
    return tests;
  }

  public void setTests(Set<Test> tests) {
    this.tests = tests;
  }
}
