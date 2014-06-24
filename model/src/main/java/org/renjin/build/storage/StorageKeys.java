package org.renjin.build.storage;

/**
 * Mappings of source/build artificats to storage keys
 */
public class StorageKeys {

  public static final String BUCKET_NAME = "renjin-build";


  public static String buildLog(int buildId, String packageVersionId) {
    return "log/" + buildId + "/" + packageVersionId.replace(':', '/') + ".log";
  }

  public static String packageSource(String groupId, String packageName, String version) {
    return "package-source/" + groupId + "/" + packageName + "_" + version + ".tar.gz";
  }
}
