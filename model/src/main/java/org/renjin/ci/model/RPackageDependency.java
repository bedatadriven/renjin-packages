package org.renjin.ci.model;


import javax.persistence.*;

@Entity
public class RPackageDependency {


  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private int id;

  @ManyToOne
  @JoinColumn(name="version_id")
  private RPackageVersion packageVersion;

  /**
   * The unqualified name of the package referenced by the
   * original package source
   */
  private String dependencyName;

  /**
   * The version specified in the package description file,
   * or null if no version has been specified
   */
  private String dependencyVersion;

  @ManyToOne
  @JoinColumn(name="dependency_id", nullable = true)
  private RPackageVersion dependency;

  private String buildScope;
  private String type;

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

  public RPackageVersion getDependency() {
    return dependency;
  }

  public void setDependency(RPackageVersion dependency) {
    this.dependency = dependency;
  }

  public String getBuildScope() {
    return buildScope;
  }

  public void setBuildScope(String buildScope) {
    this.buildScope = buildScope;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDependencyName() {
    return dependencyName;
  }

  public void setDependencyName(String dependencyName) {
    this.dependencyName = dependencyName;
  }

  public String getDependencyVersion() {
    return dependencyVersion;
  }

  public void setDependencyVersion(String dependencyVersion) {
    this.dependencyVersion = dependencyVersion;
  }
}
