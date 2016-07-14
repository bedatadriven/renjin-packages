package org.renjin.ci.jenkins.benchmark;


import com.google.common.io.ByteStreams;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class GceDescriber {
  
  public static boolean isGoogleComputeEngine() {
    try {
      InetAddress address = InetAddress.getByName("metadata.google.internal");
      System.out.println(address);
      return true;
    } catch (UnknownHostException e) {
      return false;
    }
  }
  
  public static String getGcsMachineId() {
    try {
      URL url = new URL("http://metadata.google.internal/computeMetadata/v1/instance/machine-type");
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setRequestProperty("Metadata-Flavor", "Google");
      urlConnection.setRequestMethod("GET");
      InputStream in = urlConnection.getInputStream();
      byte[] resultBytes;
      try {
        resultBytes = ByteStreams.toByteArray(in);
      } finally {
        try {
          in.close();
        } catch (Exception ignored) {
        }
      }
      String resultString = new String(resultBytes);


      // Result is something like:
      // projects/235582966705/machineTypes/n1-standard-1

      int lastSlash = resultString.lastIndexOf('/');
      String machineType = resultString.substring(lastSlash + 1);

      return "gce-" + machineType;
    } catch (Exception e) {
      throw new RuntimeException("Exception retrieving GCE machine ID: " + e.getMessage(), e);
    }
  }
}
