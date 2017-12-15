package org.renjin.ci.storage;

import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.PackageVersionId;

/**
 * Mappings of source/build artificats to storage keys
 */
public class StorageKeys {

  public static final String PACKAGE_SOURCE_BUCKET = "renjinci-package-sources";
  public static final String BUILD_LOG_BUCKET = "renjinci-logs";

  public static final String URL_ROOT = "http://storage.googleapis.com/renjinci-logs/";


  public static String buildLogUrl(PackageBuildId buildId) {
    return URL_ROOT + buildLog(buildId.getPackageVersionId(), buildId.getBuildNumber());
  }


  public static String buildLog(PackageVersionId packageVersionId, long buildNumber) {
    return buildLog(packageVersionId, "b" + buildNumber);
  }

  public static String buildLog(PackageVersionId packageVersionId, String buildId) {
    return packageVersionId.getGroupId() + "/" + packageVersionId.getPackageName() + "/" +
        packageVersionId.getPackageName() + "-" + packageVersionId.getVersionString() + "-" + buildId + ".log";
  }

  public static String buildLogUrl(PackageVersionId packageVersionId, String buildId) {
    return URL_ROOT + buildLog(packageVersionId, buildId);
  }

  public static String testLog(PackageVersionId packageVersionId, String buildId, String testName) {
    return packageVersionId.getGroupId() + "/" + packageVersionId.getPackageName() + "/" +
        packageVersionId.getPackageName() + "-" + packageVersionId.getVersionString() + "-" + buildId +
        "-" + testName + ".test.log";
  }


  public static String testLogUrl(PackageVersionId packageVersionId, String buildId, String testName) {
    return URL_ROOT + testLog(packageVersionId, buildId, testName);
  }

  public static String testLogUrl(PackageBuildId buildId, String testName) {
    return URL_ROOT + testLog(buildId.getPackageVersionId(), "b" + buildId.getBuildNumber(), testName);
  }


  public static String packageSource(String groupId, String packageName, String version) {
    return groupId + "/" + packageName + "_" + version + ".tar.gz";
  }

  public static String packageSource(PackageVersionId packageVersionId) {
    return packageSource(packageVersionId.getGroupId(), packageVersionId.getPackageName(), packageVersionId.getVersionString());
  }


  public static String packageSourceUrl(PackageVersionId packageVersionId) {
    return "http://storage.googleapis.com/" + PACKAGE_SOURCE_BUCKET + "/" +
       packageSource(packageVersionId.getGroupId(), packageVersionId.getPackageName(), packageVersionId.getVersionString());
  }
}
