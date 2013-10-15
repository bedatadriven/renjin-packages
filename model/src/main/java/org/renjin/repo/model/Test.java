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
	private RPackage rPackage;

  @OneToMany(mappedBy = "test")
  private Set<TestResult> results;


	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public RPackage getRPackage() {
		return rPackage;
	}

	public void setRPackage(RPackage pkg) {
		this.rPackage = pkg;
	}

	public void setName(String name) {
		this.name = name;
	}

  public Set<TestResult> getResults() {
    return results;
  }

  public void setResults(Set<TestResult> results) {
    this.results = results;
  }
}
