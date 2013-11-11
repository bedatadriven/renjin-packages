package org.renjin.infra.agent.test;

import io.airlift.command.Arguments;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.renjin.infra.agent.workspace.Workspace;
import org.renjin.infra.agent.workspace.WorkspaceBuilder;

import java.io.File;
import java.util.List;

@Command(name = "test", description = "Test packages in workspace")
public class TestCommand implements Runnable {

  @Option(name="-t", description = "Renjin target version to build/test against")
  private String renjinVersion;

  @Arguments
  private List<String> packages;

  @Option(name="-d", description = "location of workspace")
  private File workspaceDir = new File(".");

  @Option(name="-dev")
  private boolean devMode;

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

      TestQueue testQueue = new TestQueue(workspace);
      for(String packageName : packages) {
        testQueue.addPackage(new PackageUnderTest(packageName));
      }
      testQueue.run();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
