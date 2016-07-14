package org.renjin.ci.jenkins.benchmark;

import hudson.AbortException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Driver for "Pretty Quick R"
 */
public class PQR extends GnuR {
  public PQR(String version) {
    super(version);
  }

  @Override
  public String getId() {
    return "pqR";
  }

  @Override
  protected URL getSourceUrl() throws AbortException {
    try {
      return new URL("http://www.pqr-project.org/pqR-" + version + ".tar.gz");
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected String sourceDirectoryName() {
    return "pqR-" + version;
  }
}
