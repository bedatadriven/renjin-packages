package org.renjin.cran;

import java.io.*;

import com.google.common.io.Closeables;
import com.google.common.io.CountingOutputStream;

/**
 * Thread which collects the output of the
 * maven build process.
 */
public class OutputCollector extends Thread {
  private InputStream is;
  private File logFile;

  public static final int MAX_LOG_FILE_SIZE =  5 * 1024 * 1024;

  
  public OutputCollector(InputStream is, File logFile) {
    this.is = is;
    this.logFile = logFile;
  }

  public void run() {
    
    // make sure the log file has a dir
    logFile.getParentFile().mkdirs();
    
    CountingOutputStream out;
    try {
      FileOutputStream fos = new FileOutputStream(logFile);
      out = new CountingOutputStream(fos);
    } catch (FileNotFoundException e) {
      System.err.println("EEK: can't write to log file: " + logFile.getAbsolutePath());
      return;
    }
    try {
      InputStreamReader isr = new InputStreamReader(is);
      BufferedReader br = new BufferedReader(isr);
      String line;
      while ( (line = br.readLine()) != null) {
        out.write(line.getBytes());
        out.write((byte)'\n');

        if(out.getCount() > MAX_LOG_FILE_SIZE) {
          out.write("MAXIMUM LOGFILE SIZE REACHED - OUTPUT STOPPING\n".getBytes());
          break;
        }
      }

    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      Closeables.closeQuietly(out);
   }
  }
  
}
