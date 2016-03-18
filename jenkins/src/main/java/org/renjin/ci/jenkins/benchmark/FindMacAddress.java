package org.renjin.ci.jenkins.benchmark;

import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;

import java.io.IOException;
import java.net.NetworkInterface;

/**
 * Finds the node's MAC Address to use as a unique identifier for the box
 */
public class FindMacAddress implements Callable<String, IOException> {
  @Override
  public String call() throws IOException {

    NetworkInterface networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
    byte[] mac = networkInterface.getHardwareAddress();

    StringBuilder sb = new StringBuilder("MAC");
    for (int i = 0; i < mac.length; i++) {
      sb.append(String.format("%02X", mac[i]));
    }
    return sb.toString();
  }

  @Override
  public void checkRoles(RoleChecker roleChecker) throws SecurityException {

  }
}
