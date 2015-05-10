package org.renjin.ci.archive;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.storage.StorageKeys;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;


public class BuildLogs {

  private static final Logger LOGGER = Logger.getLogger(BuildLogs.class.getName());

  public static String tryFetchLog(PackageBuildId buildId) {

    String logUrl = "http://storage.googleapis.com/renjinci-logs/" + 
        StorageKeys.buildLog(buildId.getPackageVersionId(), buildId.getBuildNumber());
    try {
      byte[] bytes = Resources.toByteArray(new URL(logUrl));
      if(bytes.length >= 2 && bytes[0] == (byte)0x1f && bytes[1] == (byte)0x8b)
        try (Reader reader = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), Charsets.UTF_8)) {
          return CharStreams.toString(reader);
        }
      else {
        return new String(bytes, Charsets.UTF_8);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error reading " + logUrl, e);
      return null;
    }
  }

}
