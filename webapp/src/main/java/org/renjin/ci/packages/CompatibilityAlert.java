package org.renjin.ci.packages;

import com.googlecode.objectify.LoadResult;
import org.renjin.ci.datastore.PackageBuild;
import org.renjin.ci.datastore.PackageExampleResult;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.CompatibilityFlags;
import org.renjin.ci.model.PackageVersionId;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;


public class CompatibilityAlert {
  
  private String message;
  private String style;
  
  public CompatibilityAlert(PackageVersion packageVersion, LoadResult<PackageBuild> latestBuild, Iterable<PackageExampleResult> results) {
    if(packageVersion.getLastSuccessfulBuildNumber() == 0) {
      if(packageVersion.getLastBuildNumber() != 0) {
        PackageBuild lastBuild = latestBuild.safe();
        style = "danger";
        if(lastBuild.getOutcome() == BuildOutcome.BLOCKED) {
          message = "This package cannot yet be used with Renjin it depends on other packages" +
              "which are not available: " + blockingDependencyLinkList(lastBuild);
         
        } else {
          message = format("This package cannot yet be used with Renjin because there was a problem building " +
              "the package using Renjin's toolchain. <a href=\"%s\">View Build Log</a>", lastBuild.getPath());
        }
      } else {
        style = null;
        message = "This package is not yet available for use with Renjin";
      }
      
    } else {
      
      if(packageVersion.getCompatibilityFlags() == 0) {
        style = "success";
        message = "This package is available for Renjin and there are no known compatibility issues.";
      
      } else {
        style = "warning";
        message = composeWarningMessage(packageVersion);
      }
    }
  }

  private String blockingDependencyLinkList(PackageBuild lastBuild) {
    StringBuilder sb = new StringBuilder();
    List<PackageVersionId> blockers = lastBuild.getBlockingDependencyVersions();
    for (int i = 0; i < blockers.size(); i++) {
      if(i > 0) {
        if(blockers.size() > 2) {
          sb.append(", ");
        }
        if(i+1 == blockers.size()) {
          sb.append(" and ");
        }
      }
      PackageVersionId blocker = blockers.get(i);
      sb.append("<a href=\"")
          .append(blocker.getPath())
          .append("\">")
          .append(blocker.getPackageName())
          .append("&nbsp;")
          .append(blocker.getVersion())
          .append("</a>");
    }
    return sb.toString();
  }

  private static String composeWarningMessage(PackageVersion packageVersion) {
    List<String> issues = new ArrayList<>();
    if(packageVersion.getCompatibilityFlag(CompatibilityFlags.NATIVE_COMPILATION_FAILURE)) {
      issues.add("native Fortran/C/C++ sources could not be compiled");
    }
    if(packageVersion.getCompatibilityFlag(CompatibilityFlags.NO_TESTS)) {
      issues.add("no tests could be found");
    } else if(packageVersion.getCompatibilityFlag(CompatibilityFlags.TEST_FAILURES)) {
      issues.add("there were test failures");
    }

    StringBuilder sb = new StringBuilder("This package can be used with Renjin but");
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
  
  public String getMessage() {
    return message;
  }
}
