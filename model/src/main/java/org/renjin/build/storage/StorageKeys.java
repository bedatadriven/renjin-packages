package org.renjin.build.storage;

/**
 * Mappings of source/build artificats to storage keys
 */
public class StorageKeys {

  public static final String BUCKET_NAME = "renjin-build";
  public static final String PACKAGE_SOURCE_BUCKET = "renjin-ci-package-sources";
  public static final String  BUILD_LOG_BUCKET = "renjin-ci-build-logs";


  public static String buildLog(long buildId, String packageVersionId) {
    return "log/" + buildId + "/" + packageVersionId.replace(':', '/') + ".log";
  }

  public static String packageSource(String groupId, String packageName, String version) {
    return groupId + "/" + packageName + "_" + version + ".tar.gz";
  }
}
