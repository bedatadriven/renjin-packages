package org.renjin.build.model;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import java.util.Set;

@Entity
public class RPackage {

	/**
	 * Package id in the form groupId:artifactId (e.g. org.renjin.cran:lattice)
	 */
	@Id
	private String id;

	private String name;
	private String source;

	private String title;

	@Lob
	private String description;

	@OneToMany(mappedBy="rPackage")
	private Set<RPackageVersion> versions;

	@OneToMany(mappedBy="rPackage")
	private Set<Test> tests;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public Set<RPackageVersion> getVersions() {
		return versions;
	}

	public void setVersions(Set<RPackageVersion> versions) {
		this.versions = versions;
	}
  
  public RPackageVersion getLatestVersion() {

    RPackageVersion latestVersion = null;
    DefaultArtifactVersion maxVersion = null;

    for(RPackageVersion packageVersion : versions) {
      DefaultArtifactVersion version = new DefaultArtifactVersion(packageVersion.getVersion());
      if(maxVersion == null || version.compareTo(maxVersion) > 0) {
        latestVersion = packageVersion;
        maxVersion = version;
      }
    }
    return latestVersion;
  }

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<Test> getTests() {
		return this.tests;
	}

	public void setTests(Set<Test> tests) {
		this.tests = tests;
	}
}
