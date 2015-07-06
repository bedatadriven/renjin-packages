package org.renjin.ci.datastore;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfTrue;
import com.googlecode.objectify.condition.IfZero;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Entity
public class PackageVersion implements Comparable<PackageVersion> {

  @Parent
  private com.googlecode.objectify.Key<Package> packageKey; 
  
  @Id
  private String version;
  
  private String title;
  
  @Index
  private Date publicationDate;
  
  @Unindex
  private long lastBuildNumber;
  
  @Index
  private boolean needsCompilation;
  
  @Unindex
  @IgnoreSave(IfZero.class)
  private long lastSuccessfulBuildNumber;
  
  @Unindex
  private long lastExampleRun;
  
  @Unindex
  private int testFailures;
  
  @Index
  private List<String> blockingDependencies;

  /**
   * True if there is a build of this PackageVersion available. (Used for searching
   * for PackageVersions without a build)
   */
  @Index
  @IgnoreSave(IfTrue.class)
  private boolean built;
  

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
  
  
  public PackageVersionId getPackageVersionId() {
    return new PackageVersionId(getPackageId(), version);
  }

  public ArtifactVersion getVersion() {
    return new DefaultArtifactVersion(version);
  }

  public String getGroupId() {
    return getPackageVersionId().getGroupId();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public PackageDescription loadDescription() {
    return ObjectifyService.ofy()
        .load()
        .key(Key.create(packageKey, PackageVersionDescription.class, version))
        .safe()
        .parse();
  }
  
  public Supplier<PackageDescription> getDescription() {
    // start loading asynchronously
    final LoadResult<PackageVersionDescription> description = ObjectifyService.ofy()
        .load()
        .key(Key.create(packageKey, PackageVersionDescription.class, version));
    
    return Suppliers.memoize(new Supplier<PackageDescription>() {
      @Override
      public PackageDescription get() {
        return description.safe().parse();
      }
    });
  }

  public long getLastBuildNumber() {
    return lastBuildNumber;
  }

  public void setLastBuildNumber(long lastBuildNumber) {
    this.lastBuildNumber = lastBuildNumber;
  }

  public long getLastSuccessfulBuildNumber() {
    return lastSuccessfulBuildNumber;
  }
  
  public PackageBuildId getLastBuildId() {
    return new PackageBuildId(getPackageVersionId(), lastBuildNumber);
  }
  
  public String getLastSuccessfulBuildVersion() {
    if(lastSuccessfulBuildNumber > 0) {
      return version + "-b" + lastSuccessfulBuildNumber;
    } else {
      return null;
    }
  }

  public PackageBuildId getLastSuccessfulBuildId() {
    Preconditions.checkState(hasSuccessfulBuild(), "No successful builds recorded for this version");
    return new PackageBuildId(getPackageVersionId(), lastSuccessfulBuildNumber);
  }
  
  public void setLastSuccessfulBuildNumber(long lastSuccessfulBuildNumber) {
    this.lastSuccessfulBuildNumber = lastSuccessfulBuildNumber;
    this.built = (lastSuccessfulBuildNumber > 0);
  }

  public boolean hasSuccessfulBuild() {
    return lastSuccessfulBuildNumber > 0;
  }

  public Date getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(Date publicationDate) {
    this.publicationDate = publicationDate;
  }

  public boolean isBuilt() {
    return built;
  }

  public void setBuilt(boolean built) {
    this.built = built;
  }

  @OnLoad
  public void onLoad() {
    this.built = (lastSuccessfulBuildNumber > 0);
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

  public boolean isNeedsCompilation() {
    return needsCompilation;
  }

  public void setNeedsCompilation(boolean needsCompilation) {
    this.needsCompilation = needsCompilation;
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

  public int getTestFailures() {
    return testFailures;
  }

  public void setTestFailures(int testFailures) {
    this.testFailures = testFailures;
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

  @Override
  public String toString() {
    return getPackageVersionId().toString();
  }

  public static PackageVersionId idOf(Key<PackageVersion> packageVersionKey) {
    PackageId packageId = PackageId.valueOf(packageVersionKey.getParent().getName());
    String version = packageVersionKey.getName();
    
    return new PackageVersionId(packageId, version);
  }
}
