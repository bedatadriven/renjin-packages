package org.renjin.ci.datastore;

import com.fasterxml.jackson.annotation.*;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.condition.IfNull;
import org.renjin.ci.model.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Records metadata about an individual package
 * build attempt
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility=JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE, 
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PackageBuild {

  public static final int MAX_GRADE = 5;

  public static final int GRADE_A = 5;
  public static final int GRADE_B = 4;
  public static final int GRADE_C = 3;
  public static final int GRADE_D = 2;
  public static final int GRADE_F = 0;

  @Parent
  private Key<PackageVersion> versionKey;
  
  @Id
  @JsonProperty
  private long buildNumber;

  @JsonProperty
  private BuildOutcome outcome;

  @JsonProperty
  private NativeOutcome nativeOutcome;
  
  @Unindex
  private List<String> blockingDependencies;

  @Unindex
  private List<String> resolvedDependencies;
  
  /**
   * The Renjin version against which the
   * package was built
   */
  @JsonProperty
  @Unindex
  private String renjinVersion;
  
  @Index
  @IgnoreSave(IfNull.class)
  private Long startTime;

  @Index
  @IgnoreSave(IfNull.class)
  private Long endTime;

  @Unindex
  @IgnoreSave(IfNull.class)
  private Long duration;
  
  @Index
  private String grade;

  @Unindex
  private String patchId;

  public PackageBuild() {
  }

  public PackageBuild(PackageVersionId packageVersionId, long buildNumber) {
    this.versionKey = PackageVersion.key(packageVersionId);
    this.buildNumber = buildNumber;
  }

  public static Key<PackageBuild> key(PackageVersionId packageVersionId, long buildNumber) {
    return Key.create(PackageVersion.key(packageVersionId), PackageBuild.class, buildNumber);
  }

  public static Key<PackageBuild> key(PackageBuildId packageBuildId) {
    return key(packageBuildId.getPackageVersionId(), packageBuildId.getBuildNumber());
  }
  
  public static PackageBuildId idOf(Key<PackageBuild> key) {
    long buildNumber = key.getId();
    PackageVersionId packageVersionId = PackageVersion.idOf(key.<PackageVersion>getParent());
    return new PackageBuildId(packageVersionId, buildNumber);
  }

  public String getPatchUrl() {
    return String.format("https://github.com/bedatadriven/%s.%s/compare/%s...patched-%s",
        getGroupId(),
        getPackageName(),
        getVersion(),
        getVersion());
  }

  public String getLogPath() {
    return getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" +
        getPackageVersionId().getVersionString() + "-b" + buildNumber + ".log";
  }
  
  public static String getLogUrl(PackageBuildId id) {
    return "http://storage.googleapis.com/renjinci-logs/" + id.getGroupId() + "/" + id.getPackageName() + "/" +
        id.getPackageVersionId().getVersionString() + "-b" + id.getBuildNumber() + ".log";
  }
  
  @JsonProperty
  public PackageBuildId getId() {
    return new PackageBuildId(getPackageVersionId(), buildNumber);
  }


  public String getPackageName() {
    return getPackageVersionId().getPackageName();
  }

  @JsonIgnore
  public String getPath() {
    return getPackageVersionId().getPath() + "/build/" + getBuildNumber();
  }


  public String getVersion() {
    return getPackageVersionId().getVersionString();
  }

  public PackageId getPackageId() {
    return PackageId.valueOf(versionKey.getParent().getName());
  }
  
  public PackageVersionId getPackageVersionId() {
    return new PackageVersionId(getPackageId(), versionKey.getName());
  }

  public long getBuildNumber() {
    return buildNumber;
  }

  public String getBuildVersion() {
    return getVersion() + "-b" + getBuildNumber();
  }

  public String getGroupId() {
    return getPackageVersionId().getGroupId();
  }

  public boolean isSucceeded() {
    return outcome == BuildOutcome.SUCCESS;
  }

  public BuildOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(BuildOutcome outcome) {
    this.outcome = outcome;
  }

  public String getPatchId() {
    return patchId;
  }

  public void setPatchId(String patchId) {
    this.patchId = patchId;
  }

  public String getRenjinVersion() {
    return renjinVersion;
  }

  public RenjinVersionId getRenjinVersionId() {
    return new RenjinVersionId(renjinVersion);
  }

  public String getGrade() {
    return grade;
  }

  public int getGradeInteger() {
    return getGradeInteger(grade);
  }

  public static int getGradeInteger(String grade) {
    if(grade == null) {
      return GRADE_F;
    }
    switch (grade) {
      case "A":
        return GRADE_A;
      case "B":
        return GRADE_B;
      case "C":
        return GRADE_C;
      case "D":
        return GRADE_D;
      default:
      case "F":
        return GRADE_F;
    }
  }

  public void setGrade(char grade) {
    this.grade = Character.toString(grade);
  }

  public void setGrade(String grade) {
    this.grade = grade;
  }

  @JsonSetter
  public void setRenjinVersion(String renjinVersion) {
    this.renjinVersion = renjinVersion;
  }

  public void setRenjinVersion(RenjinVersionId release) {
    this.renjinVersion = release.toString();
  }

  /**
   *
   * @return the start time of a build in progress, or null if the build is complete
   */
  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public NativeOutcome getNativeOutcome() {
    return nativeOutcome;
  }

  public void setNativeOutcome(NativeOutcome nativeOutcome) {
    this.nativeOutcome = nativeOutcome;
  }

  /**
   *
   * @return the start time of a build in progress, or null if the build is complete
   */
  public Date getStartDate() {
    if(startTime != null) {
      return new Date(startTime);
    } else {
      return null;
    }
  }

  public Date getEndDate() {
    if(endTime == null) {
      return null;
    }
    return new Date(endTime);
  }

  public String getResultURL() {
    return "/package/" + getPackageVersionId().getGroupId() + "/" + getPackageName() + "/" + getVersion() +
        "/build/" + getBuildNumber();
  }

  public String getLogUrl() {
    return "//storage.googleapis.com/renjinci-logs/" + getLogPath();
  }
  

  public static Ordering<PackageBuild> orderByNumber() {
    return Ordering.natural().onResultOf(new Function<PackageBuild, Comparable>() {
      @Nullable
      @Override
      public Comparable apply(PackageBuild input) {
        return input.getBuildNumber();
      }
    });
  }

  public List<String> getBlockingDependencies() {
    return blockingDependencies;
  }
  
  public List<PackageVersionId> getBlockingDependencyVersions() {
    if(blockingDependencies == null) {
      return Collections.emptyList();
    } else {
      List<PackageVersionId> list = new ArrayList<>();
      for (String blockingDependency : blockingDependencies) {
        String[] coordinates = blockingDependency.split(":");
        if(coordinates.length >= 3) {
          list.add(new PackageVersionId(coordinates[0], coordinates[1], coordinates[2]));
        }
      }
      return list;
    }
  }

  public void setBlockingDependencies(List<String> blockingDependencies) {
    this.blockingDependencies = blockingDependencies;
  }

  public List<String> getResolvedDependencies() {
    if(resolvedDependencies != null) {
      return resolvedDependencies;
    } else {
      return Collections.emptyList();
    }
  }
  
  public List<PackageVersionId> getResolvedDependencyIds() {
    List<PackageVersionId> ids = Lists.newArrayList();
    for (String dependency : getResolvedDependencies()) {
      ids.add(PackageVersionId.fromTriplet(dependency));
    }
    return ids;
  }
  
  public void setResolvedDependencies(List<String> resolvedDependencies) {
    this.resolvedDependencies = resolvedDependencies;
  }
  
  public String getButtonStyle() {
    if(outcome == BuildOutcome.BLOCKED) {
      return "btn-blocked";
    }
    if(outcome != BuildOutcome.SUCCESS) {
      return "btn-danger";
    } else if(nativeOutcome == NativeOutcome.FAILURE) {
      return "btn-warning";
    } else {
      return "btn-success";
    }
  }
  

  /**
   *
   * @return true if the build completed (was successful, failure, error, or timeout), but
   * not null or cancelled
   */
  public boolean isFinished() {
    if(outcome == null) {
      return false;
    }

    switch (outcome) {
      case SUCCESS:
      case FAILURE:
      case ERROR:
      case TIMEOUT:
        return true;

      default:
      case BLOCKED:
      case CANCELLED:
        return false;
    }
  }

  public boolean isFailed() {
    if(outcome == null) {
      return false;
    }
    switch (outcome) {
      case ERROR:
      case FAILURE:
      case TIMEOUT:
        return true;

      default:
        return false;
    }
  }

  public static Predicate<PackageBuild> buildSucceeded() {
    return new Predicate<PackageBuild>() {
      @Override
      public boolean apply(@Nullable PackageBuild input) {
        return input.isSucceeded();
      }
    };
  }
  
  public static Predicate<PackageBuild> nativeCompilationSucceeded() {
    return new Predicate<PackageBuild>() {
      @Override
      public boolean apply(PackageBuild input) {
        return input.getNativeOutcome() == NativeOutcome.SUCCESS;
      }
    };
  }
  
  public static Predicate<PackageBuild> nativeCompilationAttempted() {
    return new Predicate<PackageBuild>() {
      @Override
      public boolean apply(PackageBuild input) {
        return input.getNativeOutcome() == NativeOutcome.SUCCESS ||
               input.getNativeOutcome() == NativeOutcome.FAILURE;
      }
    };
  }

}
