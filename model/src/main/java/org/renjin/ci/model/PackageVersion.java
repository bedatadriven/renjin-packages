package org.renjin.ci.model;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfEmpty;
import com.googlecode.objectify.condition.IfNotEmpty;
import com.googlecode.objectify.condition.IfNull;
import com.googlecode.objectify.condition.IfZero;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.joda.time.LocalDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@Entity
public class PackageVersion implements Comparable<PackageVersion> {

  @Parent
  private com.googlecode.objectify.Key<Package> packageKey; 
  
  @Id
  private String version;

  private String description;

  private LocalDateTime publicationDate;
  
  private int compatibilityLevel;
  
  private int compatibilityFlags;
  
  @Unindex
  private long lastBuildNumber;
  
  @Unindex
  @IgnoreSave(IfZero.class)
  private long lastSuccessfulBuildNumber;
  

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


  /**
   * The bioConductor release of which this package version is a release.
   */
  @IgnoreSave(IfNull.class)
  private String bioConductorRelease;
  
  public PackageVersion() {
  }

  public PackageVersion(String packageVersionId) {
    this(PackageVersionId.fromTriplet(packageVersionId)); 
  }

  public PackageVersion(PackageVersionId packageVersionId) {
    this.packageKey = Package.key(packageVersionId.getPackageId());
    this.version = packageVersionId.getVersionString();
  }


  public String getId() {
    return getPackageVersionId().toString();
  }

  /**
   * 
   * @return the renjin compatibility score
   */
  public int getCompatibilityLevel() {
    return compatibilityLevel;
  }

  public int getCompatibilityFlags() {
    return compatibilityFlags;
  }

  public void setCompatibilityFlags(int compatibilityFlags) {
    this.compatibilityFlags = compatibilityFlags;
  }
  
  public void setCompatibilityFlag(int flag) {
    this.compatibilityFlags |= flag;
  }

  public void setCompatibilityLevel(int compatibilityLevel) {
    this.compatibilityLevel = compatibilityLevel;
  }

  public PackageVersionId getPackageVersionId() {
    return new PackageVersionId(getPackageId(), version);
  }

  public ArtifactVersion getVersion() {
    return new DefaultArtifactVersion(version);
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
      throw new IllegalStateException("Could not parse DESCRIPTION for package version " + getPackageVersionId(), e);
    }
  }

  public long getLastBuildNumber() {
    return Math.max(lastBuildNumber, 200);
  }

  public void setLastBuildNumber(long lastBuildNumber) {
    this.lastBuildNumber = lastBuildNumber;
  }

  public Set<String> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Set<String> dependencies) {
    this.dependencies = dependencies;
  }

  public void setDependencies(DependencySet dependencySet) {
    this.dependencies = new HashSet<>();
    this.compileDependenciesResolved = dependencySet.isCompileDependenciesResolved();
    for(PackageVersionId id : dependencySet.getDependencies()) {
      this.dependencies.add(id.toString());
    }
  }

  public long getLastSuccessfulBuildNumber() {
    return lastSuccessfulBuildNumber;
  }


  public String getLastSuccessfulBuildVersion() {
    if(lastSuccessfulBuildNumber > 0) {
      return version + "-b" + lastSuccessfulBuildNumber;
    } else {
      return null;
    }
  }
  
  public void setLastSuccessfulBuildNumber(long lastSuccessfulBuildNumber) {
    this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
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

    DefaultArtifactVersion thisVersion = new DefaultArtifactVersion(thisId.getVersionString());
    DefaultArtifactVersion thatVersion = new DefaultArtifactVersion(thatId.getVersionString());

    return thisVersion.compareTo(thatVersion);
  }

  public Set<PackageVersionId> getDependencyIdSet() {
    Set<PackageVersionId> set = Sets.newHashSet();
    for(String id : dependencies) {
      set.add(new PackageVersionId(id));
    }
    return set;
  }
  
  public static Ordering<PackageVersion> orderByVersion() {
    return Ordering.natural().onResultOf(new Function<PackageVersion, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(PackageVersion input) {
        return input.getVersion();
      }
    });
  }

  public String getBioConductorRelease() {
    return bioConductorRelease;
  }

  public void setBioConductorRelease(String bioConductorRelease) {
    this.bioConductorRelease = bioConductorRelease;
  }

  public String getPackageName() {
    return getPackageVersionId().getPackageName();
  }

  public PackageId getPackageId() {
    return PackageId.valueOf(packageKey.getName());
  }

  public static Key<PackageVersion> key(PackageVersionId id) {
    Key<Package> packageKey = Package.key(id.getPackageId());
    return Key.create(packageKey, PackageVersion.class, id.getVersionString());
  }

  public boolean getCompatibilityFlag(int flag) {
    if( (this.compatibilityFlags & flag) != 0) {
      return true;
    } else {
      return false;
    }
  }
}
