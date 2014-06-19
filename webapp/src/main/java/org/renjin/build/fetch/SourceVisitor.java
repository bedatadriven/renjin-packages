package org.renjin.build.fetch;


import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.io.InputStream;

public interface SourceVisitor {

  public void visit(TarArchiveEntry entry, InputStream inputStream) throws IOException;

}
