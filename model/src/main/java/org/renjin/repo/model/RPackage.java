package org.renjin.repo.model;

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
}
