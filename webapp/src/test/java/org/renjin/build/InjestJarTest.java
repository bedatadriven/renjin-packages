package org.renjin.build;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Created by alex on 6/2/14.
 */
public class InjestJarTest {

  @Test
  public void jarHash() throws IOException {

    JarInputStream in = new JarInputStream(new FileInputStream(
        "/home/alex/.m2/repository/org/renjin/renjin-core/0.7.0-RC6/renjin-core-0.7.0-RC6.jar"));

    JarEntry nextJarEntry;
    while((nextJarEntry = in.getNextJarEntry())!=null) {
      HashingOutputStream out = new HashingOutputStream(Hashing.md5(), ByteStreams.nullOutputStream());
      ByteStreams.copy(in, out);
      System.out.println(nextJarEntry.getName() + ": " + out.hash().toString());
    }

  }
}
