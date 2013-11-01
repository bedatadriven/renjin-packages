package org.renjin.cran;


import java.io.File;
import java.util.concurrent.Callable;

public class PackageTester implements Runnable {

  private final Workspace workspace;
  private final BuildReporter reporter;
  private final PackageNode pkg;
  private final File baseDir;

  public PackageTester(Workspace workspace, BuildReporter reporter, PackageNode pkg) {
    this.workspace = workspace;
    this.reporter = reporter;
    this.pkg = pkg;
    this.baseDir = new File(workspace.getPackagesDir(), pkg.getName() + "_" + pkg.getVersion());
  }

  @Override
  public void run() {

    File manDir = new File(baseDir, "man");
    if(manDir.exists() && manDir.listFiles() != null) {
      for(File file : manDir.listFiles()) {
        if(file.getName().toLowerCase().endsWith(".rd")) {

        }


      }
    }




  }
}
