package org.renjin.repo.model;


import javax.persistence.*;
import java.util.Set;

@Entity
public class Test {

	@Id
  @GeneratedValue(strategy = GenerationType.AUTO)
	private int id;

	private String name;

	@ManyToOne
	private RPackageVersion packageVersion;

  @OneToMany(mappedBy = "test")
  private Set<TestResult> results;


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
