/*
 * Copyright 2002-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.renjin.cran.proxy;

import org.renjin.cran.Workspace;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Simple Maven Proxy MavenProxyServer based on DSMP. We create an isolated, local maven
 * repository for each Renjin version, but we don't want to have to "download the internet"
 * each time we build a new version, so we run our own little proxy server.
 *
 * Wait for connections from somewhere and pass them on to <code>RequestHandler</code>
 * for processing.
 *
 * @author digulla
 *
 */
public class MavenProxyServer implements Runnable
{
  public static final Logger log = Logger.getLogger(MavenProxyServer.class.getName());

  public static final int PORT = 40001;

  private Config config;
  private ServerSocket socket;


  public MavenProxyServer(Workspace workspace) throws IOException
  {
    this.config = new Config(workspace);

    log.info("Opening connection on port "+PORT);
    socket = new ServerSocket (PORT);
  }

  private static boolean run = true;

  public void terminateAll ()
  {
    // TODO Implement a way to gracefully stop the proxy
    run = false;
  }

  @Override
  public void run ()
  {
    while (run)
    {
      Socket clientSocket;

      try
      {
        clientSocket = socket.accept();
      }
      catch (IOException e)
      {
        log.log(Level.SEVERE, "Error accepting connection from client", e);
        continue;
      }

      Thread t = new RequestHandler (config, clientSocket);
      t.start();
    }

    try
    {
      socket.close();
    }
    catch (IOException e)
    {
      log.log(Level.SEVERE, "Error closing server socket", e);
    }
  }

}
