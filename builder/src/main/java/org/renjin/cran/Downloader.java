package org.renjin.cran;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Downloader {

  private File outputDir;

  private void unpack() throws IOException {



    // download package index
    File packageIndex = new File(outputDir, "index.html");
    if(!packageIndex.exists()) {
      CRAN.fetchPackageIndex(packageIndex);
    }
    List<CranPackage> cranPackages = CRAN.parsePackageList(
      Files.newInputStreamSupplier(packageIndex));

    for(CranPackage pkg : cranPackages) {
      System.out.println(pkg.getName());
      String pkgName = pkg.getName().trim();
      if(!Strings.isNullOrEmpty(pkgName)) {
        File pkgRoot = new File(outputDir, pkgName);
        CRAN.unpackSources(pkg, pkgRoot);
      }
    }
  }


}
