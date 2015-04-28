package org.renjin.ci.packages;

import org.renjin.ci.model.CompatibilityFlags;
import org.renjin.ci.datastore.PackageVersion;

import java.util.ArrayList;
import java.util.List;


public class CompatibilityAlert {
  
  private String message;
  private String style;
  
  public CompatibilityAlert(PackageVersion packageVersion) {
    if(packageVersion.getLastSuccessfulBuildNumber() == 0) {
      if(packageVersion.getCompatibilityFlag(CompatibilityFlags.BUILD_FAILURE)) {
        style = "danger";
        message = "There was a problem building this package.";
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
      return "alert";
    } else {
      return "alert alert-" + style;
    }
  }
  
  public String getMessage() {
    return message;
  }
}
