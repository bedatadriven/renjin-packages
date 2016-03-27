package org.renjin.ci.storage;

import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

/**
 * Mappings of source/build artificats to storage keys
 */
public class StorageKeys {

  public static final String PACKAGE_SOURCE_BUCKET = "renjinci-package-sources";
  public static final String BUILD_LOG_BUCKET = "renjinci-logs";


  public static String buildLogUrl(PackageBuildId buildId) {
    return  "http://storage.googleapis.com/renjinci-logs/" +
        buildLog(buildId.getPackageVersionId(), buildId.getBuildNumber());
  }

  public static String buildLog(String packageVersionId, long buildId) {
    return buildLog(PackageVersionId.fromTriplet(packageVersionId), buildId);
  }
  


  public static String buildLog(PackageVersionId packageVersionId, long buildNumber) {
    return packageVersionId.getGroupId() + "/" + packageVersionId.getPackageName() + "/" + 
        packageVersionId.getPackageName() + "-" + packageVersionId.getVersionString() + "-b" + buildNumber + ".log";
  }

  public static String testLog(PackageVersionId packageVersionId, long buildNumber, String testName) {
    return packageVersionId.getGroupId() + "/" + packageVersionId.getPackageName() + "/" +
        packageVersionId.getPackageName() + "-" + packageVersionId.getVersionString() + "-b" + buildNumber +
        "-" + testName + ".test.log";
  }

  public static String packageSource(String groupId, String packageName, String version) {
    return groupId + "/" + packageName + "_" + version + ".tar.gz";
  }

  public static String packageSource(PackageVersionId packageVersionId) {
    return packageSource(packageVersionId.getGroupId(), packageVersionId.getPackageName(), packageVersionId.getVersionString());
  }
}
