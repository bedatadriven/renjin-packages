package org.renjin.ci.datastore;


import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;

public class FunctionUseResults {

  private static final int PAGE_SIZE = 50;

  private FunctionUseResults(QueryResultIterator<Key<FunctionIndex>> resultIt) {

  }

  private static String packageFileOf(String keyName) {
    int versionStart = keyName.indexOf('@');
    return keyName.substring(0, versionStart);
  }
  
  private static PackageVersionId versionOf(String keyName) {
    int filenameStart = keyName.indexOf('/');
    int versionStart = keyName.indexOf('@');

    PackageId packageId = PackageId.valueOf(keyName.substring(0, filenameStart));
    String version = keyName.substring(versionStart+1);
    return new PackageVersionId(packageId, version);
  }

  public static FunctionUseResults fromIterator(QueryResultIterator<Key<FunctionIndex>> resultIt) {
//
//    Map<String, PackageVersionId> latestVersions = Maps.newHashMap();
//    
//    while(resultIt.hasNext()) {
//
//      // Key is in the format
//      // packageId/filename@version
//      Key<FunctionIndex> key = resultIt.next();
//      
//      String fileKey = packageFileOf(key.getName());
//      PackageVersionId version = versionOf(key.getName());
//
//      if(latestVersions.containsKey(fileKey)) {
//        PackageVersionId latestVersion = latestVersions.get(fileKey);
//        if(version.isNewer(latestVersion)) {
//          latestVersions.put(fileKey, version);
//        }
//      
//      } else {
//        if(latestVersions.size() > PAGE_SIZE) {
//          
//        }
//        
//        
//      }
//      
//      
//      if(fileKey.equals(currentFileKey)) {
//        // We've already seen a version of this file, is this one newer?
//        if(version.isNewer(latestVersion)) {
//          latestVersion = version;
//        }
//      } else {
//        // This is the first time we're seeing this package, add the previous one if
//        // one exists with the latest version that we found.
//        if(!currentFileKey.isEmpty()) {
//          int filenameStart = fileKey.indexOf('/');
//          String filename = 
//          sourceKeys.add(PackageSource.key(latestVersion, fileKey))
//        }
//        
//        // update the new current
//      }

    throw new UnsupportedOperationException();
  }
}

