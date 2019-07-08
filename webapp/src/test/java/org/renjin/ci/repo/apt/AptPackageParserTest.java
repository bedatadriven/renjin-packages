package org.renjin.ci.repo.apt;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@Ignore
public class AptPackageParserTest {

  @Test
  public void test() throws IOException {
    AptPackageParser parser = new AptPackageParser();
    File file = new File("/home/alex/Downloads/renjin-debian-package-0.9.2720.deb");
    AptArtifact artifact;
    try(InputStream in = new MockInputStream(new FileInputStream(file))) {
      artifact = parser.parsePackage("object", in);
    }
    assertThat(artifact.getControlFile(), not(nullValue()));
    assertThat(artifact.getName(), equalTo("Renjin"));
  }

  private class MockInputStream extends InputStream {

    private InputStream delegate;

    public MockInputStream(InputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }
  }



}