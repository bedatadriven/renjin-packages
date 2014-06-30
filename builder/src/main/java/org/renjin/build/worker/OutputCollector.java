package org.renjin.build.worker;

import com.google.common.io.CountingOutputStream;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread which collects the output of the
 * maven build process.
 */
public class OutputCollector extends Thread {

  private static final Logger LOGGER = Logger.getLogger(OutputCollector.class.getName());

  private final int maxSize;
  private File logFile;
  private Process process;

  
  public OutputCollector(Process process, File logFile, int maxSize) {
    this.process = process;
    this.logFile = logFile;
    this.maxSize = maxSize;
  }

  public void run() {
    
    try(BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
        CountingOutputStream out = new CountingOutputStream(new FileOutputStream(logFile))) {

      String line;
      while ( (line = in.readLine()) != null) {
        out.write(line.getBytes());
        out.write((byte)'\n');

        if(out.getCount() > maxSize) {
          out.write("MAXIMUM LOGFILE SIZE REACHED - OUTPUT STOPPING\n".getBytes());
          break;
        }
      }

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Exception while writing build log", e);
    }
  }
  
}