package org.renjin.cran.proxy;


import io.airlift.command.Command;
import org.renjin.infra.agent.workspace.Workspace;

import java.io.File;

@Command(name = "proxy", description = "Run a maven proxy server")
public class ProxyCommand implements Runnable {
  @Override
  public void run() {

    try {
      Workspace workspace = new Workspace(new File("."));
      MavenProxyServer server = new MavenProxyServer(workspace);
      server.run();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
