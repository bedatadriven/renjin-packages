package org.renjin.repo.task;


import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.repo.model.RPackageVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.zip.GZIPInputStream;

public interface SourceVisitor {

  public void visit(TarArchiveEntry entry, InputStream inputStream) throws IOException;

}
