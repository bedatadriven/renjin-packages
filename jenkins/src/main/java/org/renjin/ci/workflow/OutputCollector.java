package org.renjin.ci.workflow;

import com.google.common.io.Closer;
import com.google.common.io.CountingOutputStream;
import org.renjin.ci.model.NativeOutcome;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread which collects the output of the
 * maven build process.
 */
public class OutputCollector implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(OutputCollector.class.getName());

  private final int maxSize;
  private File logFile;
  private Process process;

  private NativeOutcome nativeOutcome = NativeOutcome.NA;

  public OutputCollector(Process process, File logFile, int maxSize) {
    this.process = process;
    this.logFile = logFile;
    this.maxSize = maxSize;
  }

  public void run() {

    try {
      Closer closer = Closer.create();
      try {
        BufferedReader in = closer.register(new BufferedReader(new InputStreamReader(process.getInputStream())));
        CountingOutputStream out = new CountingOutputStream(new FileOutputStream(logFile));

        String line;
        while ( (line = in.readLine()) != null) {

          if(line.startsWith("Soot finished on ")) {
            nativeOutcome = NativeOutcome.SUCCESS;
          } else if(line.contains("Compilation of GNU R sources failed")) {
            nativeOutcome = NativeOutcome.FAILURE;
          }

          out.write(line.getBytes());
          out.write((byte)'\n');

          if(out.getCount() > maxSize) {
            // there was an infinite loop bug that was filling up the harddrives of workers!
            out.write("MAXIMUM LOGFILE SIZE REACHED - OUTPUT STOPPING\n".getBytes());
            break;
          }
        }

      } catch (Exception e) {
        throw closer.rethrow(e);
      } finally {
        closer.close();
      }

    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Exception while writing build log", e);
    }
  }

  public NativeOutcome getNativeOutcome() {
    return nativeOutcome;
  }
}
