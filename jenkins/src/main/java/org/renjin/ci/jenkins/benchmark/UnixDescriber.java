package org.renjin.ci.jenkins.benchmark;

import hudson.remoting.Callable;
import org.jenkinsci.remoting.RoleChecker;
import org.renjin.ci.model.MachineDescriptor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.NetworkInterface;

/**
 * Interface to gathering information about the machine on which bencharks are run
 */
public class UnixDescriber implements Callable<MachineDescriptor, IOException> {
  @Override
  public MachineDescriptor call() throws IOException {
    
    MachineDescriptor descriptor = new MachineDescriptor();
    descriptor.setId(findMacId());
    descriptor.setOperatingSystem(System.getProperty("os.name"));
    descriptor.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
    descriptor.setPhysicalMemory(findPhysicalMemory());
    descriptor.setCpuModel(findCpuModel());
    return descriptor;
  }

  private String findMacId() throws IOException {

    try {
      NetworkInterface networkInterface = NetworkInterface.getNetworkInterfaces().nextElement();
      byte[] mac = networkInterface.getHardwareAddress();

      StringBuilder sb = new StringBuilder("MAC");
      for (int i = 0; i < mac.length; i++) {
        sb.append(String.format("%02X", mac[i]));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new IOException("Could not read MAC address for ID", e);
    }
  }

  @Override
  public void checkRoles(RoleChecker roleChecker) throws SecurityException {

  }
  
  private long findPhysicalMemory() throws IOException {
    try {
      BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
      String line;
      while((line=reader.readLine()) != null) {
        if(line.startsWith("MemTotal:")) {
          String memory = line.substring("MemTotal:".length()).trim();
          if(memory.endsWith("kB")) {
            String kb = memory.substring(0, memory.length() - "kb".length()).trim();
            return Long.parseLong(kb) * 1024;
          }
        }
      }
    } catch (IOException e) {
      throw new IOException("Could not obtain physical memory size: " + e.getMessage(), e);
    }
    return 0;
  }
  
  private String findCpuModel() throws IOException {
    try {
      BufferedReader reader = new BufferedReader(new FileReader("/proc/cpuinfo"));
      String line;
      while((line=reader.readLine()) != null) {
        String[] keyValue = line.split(":", 2);
        if(keyValue[0].trim().equals("model name")) {
          return keyValue[1].trim();
        }
      }
    } catch (IOException e) {
      throw new IOException("Could not obtain physical memory size: " + e.getMessage(), e);
    }
    return "Unknown";
  }
}
