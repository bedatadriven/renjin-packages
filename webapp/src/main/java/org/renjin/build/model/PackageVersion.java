package org.renjin.build.model;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfEmpty;
import com.googlecode.objectify.condition.IfFalse;
import com.googlecode.objectify.condition.IfNotEmpty;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.joda.time.LocalDateTime;

import javax.annotation.Nonnull;
import java.util.Set;

@Entity
public class PackageVersion implements Comparable<PackageVersion> {

  @Id
  private String id;

  private String description;

  private LocalDateTime publicationDate;

  /**
   * List of PackageVersion ids on which this package depends.
   */
  @Index(IfNotEmpty.class)
  @IgnoreSave(IfEmpty.class)
  private Set<String> dependencies = Sets.newHashSet();

  /**
   * True if we have the source of all of this packages'
   * compile time dependencies
   */
  private boolean compileDependenciesResolved;

  public PackageVersion() {
  }

  public PackageVersion(PackageVersionId packageVersionId) {
    this.id = packageVersionId.toString();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }


  public PackageVersionId getPackageVersionId() {
    return PackageVersionId.fromTriplet(id);
  }

  public ArtifactVersion getVersion() {
    return new DefaultArtifactVersion(getPackageVersionId().getSourceVersion());
  }

  public String getGroupId() {
    return getPackageVersionId().getGroupId();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PackageDescription parseDescription() {
    try {
      return PackageDescription.fromString(description);
    } catch (Exception e) {
      throw new IllegalStateException("Could not parse DESCRIPTION for package version " + id, e);
    }
  }


  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

  public LocalDateTime getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(LocalDateTime publicationDate) {
    this.publicationDate = publicationDate;
  }

  public boolean isCompileDependenciesResolved() {
    return compileDependenciesResolved;
  }

  public void setCompileDependenciesResolved(boolean compileDependenciesResolved) {
    this.compileDependenciesResolved = compileDependenciesResolved;
  }

  @OnLoad
  public void onLoad() {
    if(dependencies == null) {
      dependencies = Sets.newHashSet();
    }
  }

  @Override
  public int compareTo(@Nonnull PackageVersion o) {
    PackageVersionId thisId = PackageVersionId.fromTriplet(this.getId());
    PackageVersionId thatId = PackageVersionId.fromTriplet(o.getId());

    if(!thisId.getGroupId().equals(thatId.getGroupId())) {
      return thisId.getGroupId().compareTo(thatId.getGroupId());
    }

    if(!thisId.getPackageName().equals(thatId.getPackageName())) {
      return thisId.getPackageName().compareTo(thatId.getPackageName());
    }

    DefaultArtifactVersion thisVersion = new DefaultArtifactVersion(thisId.getSourceVersion());
    DefaultArtifactVersion thatVersion = new DefaultArtifactVersion(thatId.getSourceVersion());

    return thisVersion.compareTo(thatVersion);
  }

  public Set<PackageVersionId> getDependencyIdSet() {
    Set<PackageVersionId> set = Sets.newHashSet();
    for(String id : dependencies) {
      set.add(new PackageVersionId(id));
    }
    return set;
  }
}
