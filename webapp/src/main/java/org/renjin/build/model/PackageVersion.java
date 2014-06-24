package org.renjin.build.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;
import java.util.List;

@Entity
public class PackageVersion {

  @Id
  private String id;

  private String description;

  private boolean latest;

  @Index
  private long lastSuccessfulBuild;

  private Date publicationDate;

  /**
   * The version of GNU R on which this package depends
   */
  private String gnuRDependency;

  /**
   * List of PackageVersion ids on which this package depends.
   */
  @Index
  private List<String> dependencies;


  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public boolean isLatest() {
    return latest;
  }

  public void setLatest(boolean latest) {
    this.latest = latest;
  }

  public long getLastSuccessfulBuild() {
    return lastSuccessfulBuild;
  }

  public void setLastSuccessfulBuild(long lastSuccessfulBuild) {
    this.lastSuccessfulBuild = lastSuccessfulBuild;
  }

  public String getGnuRDependency() {
    return gnuRDependency;
  }

  public void setGnuRDependency(String gnuRDependency) {
    this.gnuRDependency = gnuRDependency;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<String> dependencies) {
    this.dependencies = dependencies;
  }

  public Date getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(Date publicationDate) {
    this.publicationDate = publicationDate;
  }
}
