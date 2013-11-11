package org.renjin.infra.agent.build;

import java.io.File;
import java.util.*;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;

import org.renjin.infra.agent.workspace.Workspace;
import org.renjin.infra.agent.workspace.WorkspaceBuilder;
import org.renjin.repo.model.BuildOutcome;

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

  @Option(name="-dev")
  private boolean devMode;

  @Option(name="-t", description = "Renjin target version to build/test against")
  private String renjinVersion;

  @Arguments
  private List<String> packages;

  private Reactor packageBuilder;

  @Override
  public void run() {

    try {

      WorkspaceBuilder workspaceBuilder = new WorkspaceBuilder(workspaceDir);

      if(!devMode) {
        if(renjinVersion != null) {
          workspaceBuilder.setRenjinVersion(renjinVersion);
        } else {
          workspaceBuilder.setRenjinVersion("master");
        }
      }

      Workspace workspace = workspaceBuilder.build();
      workspace.setDevMode(devMode);

      System.out.println("Testing against " + workspace.getRenjinVersion());

      buildRenjin(workspace);

      GraphBuilder graphBuilder = new GraphBuilder();
      if(packages.contains("ALL")) {
        graphBuilder.addAllLatestVersions();
      } else {
        for(String packageName : packages) {
          try {
            graphBuilder.addPackage(packageName);
          } catch(UnresolvedDependencyException ude) {
            System.out.println("Could not add " + packageName + " because of unresolved dependency: " +
              ude.getPackageName());
          }
        }
      }
      packageBuilder = new Reactor(workspace, graphBuilder.build());
      packageBuilder.setNumConcurrentBuilds(numConcurrentBuilds);
      packageBuilder.build();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private void buildRenjin(Workspace workspace) throws Exception {
    if(!devMode && workspace.isSnapshot() && workspace.getRenjinBuildOutcome() != BuildOutcome.SUCCESS) {
      
//      System.out.println("Starting proxy server...");
//      Thread thread = new Thread(new MavenProxyServer(workspace));
//      thread.start();
//      Thread.sleep(1000);
//
      System.out.println("Building Renjin...");

      RenjinBuilder renjinBuilder = new RenjinBuilder(workspace);
      BuildOutcome result = renjinBuilder.call();
      System.out.println("Renjin build complete: " + result);

//      System.out.println("Shutting down proxy server...");
//      thread.interrupt();
//
      if(result != BuildOutcome.SUCCESS) {
        System.out.println("Renjin build failed! Abandoning ship...");
        System.exit(-1);
      }
    }
  }
}
