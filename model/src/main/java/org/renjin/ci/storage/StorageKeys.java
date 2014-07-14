package org.renjin.ci.storage;

/**
 * Mappings of source/build artificats to storage keys
 */
public class StorageKeys {

  public static final String PACKAGE_SOURCE_BUCKET = "renjinci-package-sources";
  public static final String BUILD_LOG_BUCKET = "renjinci-logs";


  public static String buildLog(long buildId, String packageVersionId) {
    return buildId + "/" + packageVersionId.replace(':', '/') + ".log";
  }

  public static String packageSource(String groupId, String packageName, String version) {
    return groupId + "/" + packageName + "_" + version + ".tar.gz";
  }
}
