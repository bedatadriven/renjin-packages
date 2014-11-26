package org.renjin.ci.test;

import com.google.appengine.api.taskqueue.DeferredTask;
import com.googlecode.objectify.ObjectifyService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.renjin.ci.model.PackageTest;
import org.renjin.ci.index.dependencies.ExamplesParser;
import org.renjin.parser.RdParser;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExtractTestTask implements DeferredTask {
  private final boolean compressed;
  private final byte bytes[];
  private final String path;
  private final String group;
  private final String artifact;

  private static final int maxLength = 100000;  // Compress byte array above this length

  public ExtractTestTask(byte bytes[], String path, String group, String artifact) {
    boolean compressed = false;
    if (bytes.length > maxLength) {
      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        try (BZip2CompressorOutputStream bZip2CompressorOutputStream = new BZip2CompressorOutputStream(outputStream)) {
          bZip2CompressorOutputStream.write(bytes, 0, bytes.length);
        }
        bytes = outputStream.toByteArray();
        compressed = true;
      } catch (Exception e) {/* Just use the uncompressed data and hope for the best */}
    }
    this.compressed = compressed;
    this.bytes = bytes;
    this.path = path;
    this.group = group;
    this.artifact = artifact;
  }

  private InputStreamReader getInputStreamReader() throws IOException {
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    if (compressed) {
      BZip2CompressorInputStream bZip2CompressorInputStream = new BZip2CompressorInputStream(byteArrayInputStream);
      return new InputStreamReader(bZip2CompressorInputStream);
    }
    else return new InputStreamReader(byteArrayInputStream);
  }

  public void run() {
    try (InputStreamReader reader = getInputStreamReader()) {
      RdParser parser = new RdParser();
      SEXP rd = parser.R_ParseRd(reader, StringVector.valueOf(path), false);

      ExamplesParser examples = new ExamplesParser();
      rd.accept(examples);

      String source = examples.getResult();

      if (source.trim().isEmpty()) return;

      PackageTest packageTest = new PackageTest();
      packageTest.setId(group + ':' + artifact + ':' + DigestUtils.md5Hex(source));
      packageTest.setPath(path);
      packageTest.setSource(source);

      ObjectifyService.ofy().save().entity(packageTest).now();
    } catch(Throwable t) {
      System.err.printf("WARNING: Failed to parse examples from %s:%s:%s. [%s]", group, artifact, path, t.getMessage());
    }
  }
}
