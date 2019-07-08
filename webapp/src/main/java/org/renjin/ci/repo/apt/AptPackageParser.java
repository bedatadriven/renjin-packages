/*
 * Nexus APT plugin.
 * 
 * Copyright (c) 2016-Present Michael Poindexter.
 * 
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */

package org.renjin.ci.repo.apt;

import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.renjin.ci.model.PackageDescription;
import org.renjin.ci.storage.StorageKeys;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.logging.Logger;

public class AptPackageParser {

  private static final Logger LOGGER = Logger.getLogger(AptRepository.class.getName());

  private final GcsService gcsService =
      GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

  private Hasher sha1 = Hashing.sha1().newHasher();
  private Hasher sha256 = Hashing.sha256().newHasher();
  private Hasher sha512 = Hashing.sha512().newHasher();
  private Hasher md5 = Hashing.md5().newHasher();

  private long totalBytesRead;

  private String controlFileString;

  private class HashingInputStream extends InputStream {

    private final InputStream in;

    public HashingInputStream(InputStream in) {
      this.in = in;
    }

    public int read() throws IOException {
      int b = this.in.read();
      if (b != -1) {
        byte bi = (byte) b;
        sha1.putByte(bi);
        sha256.putByte(bi);
        sha512.putByte(bi);
        md5.putByte(bi);
        totalBytesRead++;
      }

      return b;
    }

    public int read(byte[] bytes, int off, int len) throws IOException {
      int bytesRead = in.read(bytes, off, len);
      if(bytesRead < 0) {
        return -1;
      }
      sha1.putBytes(bytes, off, bytesRead);
      sha256.putBytes(bytes, off, bytesRead);
      sha512.putBytes(bytes, off, bytesRead);
      md5.putBytes(bytes, off, bytesRead);
      totalBytesRead += bytesRead;

      return bytesRead;
    }



    public boolean markSupported() {
      return false;
    }

    public void mark(int readlimit) {
    }

    public void reset() throws IOException {
      throw new IOException("reset not supported");
    }
  }

  public AptArtifact parsePackage(String objectName) throws IOException {
    GcsFilename filename = new GcsFilename(StorageKeys.REPO_BUCKET, "deb/" + objectName);

    LOGGER.info("Reading " + filename.toString());

    try (InputStream in = Channels.newInputStream(
        gcsService.openPrefetchingReadChannel(filename, 0, 1024 * 1024))) {

      return parsePackage(objectName, in);

    }
  }

  @VisibleForTesting
  AptArtifact parsePackage(String objectName, InputStream in) throws IOException {

    ArArchiveInputStream is = new ArArchiveInputStream(
        new BufferedInputStream(
          new HashingInputStream(in)));

    ArchiveEntry debEntry;
    while ((debEntry = is.getNextEntry()) != null) {

      LOGGER.info("debEntry = " + debEntry.getName());

      InputStream controlStream;
      switch (debEntry.getName()) {
        case "control.tar":
          controlStream = new CloseShieldInputStream(is);
          break;
        case "control.tar.gz":
          controlStream = new GzipCompressorInputStream(new CloseShieldInputStream(is));
          break;
        case "control.tar.xz":
          throw new UnsupportedOperationException("XZ compression");
        default:
          continue;
      }

      try (TarArchiveInputStream controlTarStream = new TarArchiveInputStream(controlStream)) {
        ArchiveEntry tarEntry;
        while ((tarEntry = controlTarStream.getNextEntry()) != null) {
          if (tarEntry.getName().equals("control") || tarEntry.getName().equals("./control")) {
            controlFileString = new String(ByteStreams.toByteArray(controlTarStream), Charsets.UTF_8);
          }
        }
      }
    }
    return buildArtifact(objectName);
  }

  private AptArtifact buildArtifact(String objectName) throws IOException {

    PackageDescription controlFile = PackageDescription.fromString(this.controlFileString);

    AptArtifact artifact = new AptArtifact();
    artifact.setObjectName(objectName);
    artifact.setName(controlFile.getPackage());
    artifact.setFilename(controlFile.getPackage().toLowerCase() + "-" + controlFile.getVersion() + ".deb");
    artifact.setVersion(controlFile.getVersion());
    artifact.setControlFile(controlFileString);
    artifact.setArchitecture(controlFile.getFirstProperty("Architecture"));
    artifact.setSize(totalBytesRead);
    artifact.setSha1(sha1.hash().toString());
    artifact.setSha256(sha256.hash().toString());
    artifact.setSha512(sha512.hash().toString());
    artifact.setMd5(md5.hash().toString());

    return artifact;
  }
}
