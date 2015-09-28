package org.renjin.ci.packages;

import com.google.common.collect.Iterables;
import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageVersionId;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;


public class CompatibilityAlert {

  private final PackageVersion packageVersion;
  private final LoadResult<PackageBuild> latestBuild;
  private final Iterable<PackageTestResult> results;
  private String message;
  private String style;

  public CompatibilityAlert(PackageVersion packageVersion, LoadResult<PackageBuild> latestBuild, Iterable<PackageTestResult> results) {
    this.packageVersion = packageVersion;
    this.latestBuild = latestBuild;
    this.results = results;
  }

  public String getMessage() {
    if(packageVersion.getLastBuildNumber() == 0) {
      return "This package has not yet been built and tested against Renjin. Please allow a few days for the " +
          "package to be built.";
    }

    PackageBuild lastBuild = latestBuild.safe();

    if(lastBuild.getOutcome() == BuildOutcome.BLOCKED) {
      return "This package cannot yet be used with Renjin it depends on other packages " +
          "which are not available: " + blockingDependencyLinkList(lastBuild);

    } else if(lastBuild.getOutcome() != BuildOutcome.SUCCESS) {
      return format("This package cannot yet be used with Renjin because there was a problem building " +
          "the package using Renjin's toolchain. <a href=\"%s\">View Build Log</a>", lastBuild.getPath());
    
    } else {
      List<String> issues = new ArrayList<>();
      
      if(lastBuild.getNativeOutcome() == NativeOutcome.FAILURE) {
        issues.add("there was an error compiling C/Fortran sources");
      }
      int testCount = Iterables.size(results);
      int testFailures = countFailures(results);
      
      if(testCount == 0) {
        issues.add("no tests could be found for the package");
        
      } else if(testFailures == testCount) {
        issues.add("all tests failed");
        
      } else if(testFailures > 0) {
        issues.add(format("%d out %d tests failed", testFailures, testCount));
      }
      
      if(issues.size() > 0) {
        return composeWarningMessage(issues);
      } else {
        return  "This package is available for Renjin and there are no known compatibility issues.";
      }
    }
  }

  private int countFailures(Iterable<PackageTestResult> results) {
    int count = 0;
    for (PackageTestResult result : results) {
      if(!result.isPassed()) {
        count++;
      }
    }
    return count;
  }

  private String blockingDependencyLinkList(PackageBuild lastBuild) {
    StringBuilder sb = new StringBuilder();
    List<String> blockers = lastBuild.getBlockingDependencies();
    for (int i = 0; i < blockers.size(); i++) {
      if(i > 0) {
        if(blockers.size() > 2) {
          sb.append(", ");
        }
        if(i+1 == blockers.size()) {
          sb.append(" and ");
        }
      }
      String[] coordinates = blockers.get(i).split(":");
      if(coordinates.length >= 3) {
        PackageVersionId blocker = new PackageVersionId(coordinates[0], coordinates[1], coordinates[2]);
        sb.append("<a href=\"")
            .append(blocker.getPath())
            .append("\">")
            .append(blocker.getPackageName())
            .append("&nbsp;")
            .append(blocker.getVersion())
            .append("</a>");
      } else {
        // unqualified package name
        sb.append(coordinates[0]);
      }
    }
    return sb.toString();
  }

  private static String composeWarningMessage(List<String> issues) {
    StringBuilder sb = new StringBuilder("This package can be loaded by Renjin but");
    for(int i=0; i<issues.size();++i) {
      if(i > 0) {
        if(i+1 == issues.size()) {
          sb.append(" and");
        } else {
          sb.append(",");
        }
      }
      sb.append(" ").append(issues.get(i));
    }
    sb.append(".");
    return sb.toString();
  }

  public String getAlertStyle() {
    if(style == null) {
      return "note";
    } else {
      return "note note-" + style;
    }
  }
}
