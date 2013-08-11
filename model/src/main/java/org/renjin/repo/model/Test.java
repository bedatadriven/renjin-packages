package org.renjin.repo.model;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

@Entity
public class Test {

	@Id
	private int id;

	private String name;

	@ManyToMany
	private RPackageVersion packageVersion;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public RPackageVersion getPackageVersion() {
		return packageVersion;
	}

	public void setPackageVersion(RPackageVersion packageVersion) {
		this.packageVersion = packageVersion;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
