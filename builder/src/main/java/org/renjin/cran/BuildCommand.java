package org.renjin.cran;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import org.renjin.cran.proxy.MavenProxyServer;
import org.renjin.repo.model.BuildOutcome;
import org.renjin.repo.model.RPackage;
import org.renjin.repo.model.RPackageVersion;

import javax.persistence.EntityManager;

/**
 * Program that will retrieve package sources from CRAN,
 * build, and report results.
 */
@Command(name = "build", description = "Build packages in workspace")
public class BuildCommand implements Runnable {

  @Option(name="-d", description = "location of workspace")
  private File workspaceDir = new File(".");

  @Option(name="-j", description = "number of concurrent builds")
  private int numConcurrentBuilds = 1;

  @Option(name="-t", description = "Renjin target version to build/test against")
  private String renjinVersion;

  @Arguments
  private List<String> packages;

  private PackageGraphBuilder packageBuilder;

  @Override
  public void run() {

    try {

      WorkspaceBuilder workspaceBuilder = new WorkspaceBuilder(workspaceDir);

      if(renjinVersion != null) {
        workspaceBuilder.setRenjinVersion(renjinVersion);
      } else {
        workspaceBuilder.setRenjinVersion("master");
      }

      Workspace workspace = workspaceBuilder.build();

      System.out.println("Testing against " + workspace.getRenjinVersion());

      buildRenjin(workspace);

      packageBuilder = new PackageGraphBuilder(workspace);

      EntityManager em = PersistenceUtil.createEntityManager();
      em.getTransaction().begin();

      if(packages.contains("ALL")) {
        List<RPackageVersion> all = em.createQuery("select v from RPackageVersion v", RPackageVersion.class).getResultList();
        System.out.println("Building " + all.size() + " packages");
        for(RPackageVersion version : all) {
          try {
            File packageDir = ensureUnpacked(version);
            packageBuilder.addPackage(new PackageNode(packageDir));
          } catch(Exception e) {
            System.err.println("...Exception unpacking " + version.getPackageName() + " " +
              version.getVersion() + ": " + e.getMessage());
          }
        }
      } else {
        for(String packageName : packages) {
          RPackageVersion latestVersion = queryPackageVersion(em, packageName);

          System.out.println("Building " + latestVersion.getPackageName() + " " + latestVersion.getVersion());

          File packageDir = ensureUnpacked(latestVersion);

          packageBuilder.addPackage(new PackageNode(packageDir));

        }
      }
      em.close();

      packageBuilder.build();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void buildRenjin(Workspace workspace) throws Exception {
    if(workspace.isSnapshot() && workspace.getRenjinBuildOutcome() != BuildOutcome.SUCCESS) {
      
      System.out.println("Starting proxy server...");
      Thread thread = new Thread(new MavenProxyServer(workspace));
      thread.start();
      Thread.sleep(1000);
      
      System.out.println("Building Renjin...");

      RenjinBuilder renjinBuilder = new RenjinBuilder(workspace);
      BuildOutcome result = renjinBuilder.call();
      System.out.println("Renjin build complete: " + result);

      System.out.println("Shutting down proxy server...");
      thread.interrupt();
      
      if(result != BuildOutcome.SUCCESS) {
        System.exit(-1);
      }
    }
  }

  private RPackageVersion queryPackageVersion(EntityManager em, String packageName) {
    String qualifiedName = "org.renjin.cran:" + packageName;
    RPackage rPackage = em.find(RPackage.class, qualifiedName);
    if(rPackage == null) {
      throw new RuntimeException("Cannot find package " + qualifiedName);
    }
    return rPackage.getLatestVersion();
  }

  /**
   * Check whether the source is already unpacked in our workspace,
   * otherwise download and unpack
   * @param version
   */
  private File ensureUnpacked(RPackageVersion version) throws IOException {
    String dirName = version.getPackageName() + "_" + version.getVersion();
    File packagesDir = new File(workspaceDir, "packages");
    File dir = new File(packagesDir, dirName);

    if(!dir.exists()) {

      URL url = new URL("http://commondatastorage.googleapis.com/package-sources/cran/" +
        version.getPackageName() + "_" + version.getVersion() + ".tar.gz");

      System.out.println("Fetching " + url + "...");

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      int code = connection.getResponseCode();
      if(code != 200) {
        throw new IOException("Could not fetch source " + url + ": " + code);
      }
      TarArchiveInputStream tarIn = new TarArchiveInputStream(
        new GZIPInputStream(
          connection.getInputStream()));

      String packagePrefix = version.getPackageName() + "/";

      TarArchiveEntry entry;
      while((entry=tarIn.getNextTarEntry())!=null) {
        if(entry.isFile() && entry.getName().startsWith(packagePrefix)) {

          String name = entry.getName().substring(packagePrefix.length());

          File outFile = new File(dir.getAbsolutePath() + File.separator + name);
          outFile.getParentFile().mkdirs();

          ByteStreams.copy(tarIn, Files.newOutputStreamSupplier(outFile));
        }
      }
      tarIn.close();
    }
    return dir;
  }
}
