package org.renjin.ci.jenkins.benchmark;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * Intel Math Kernel Library.
 *
 * <p>Expects MKL to be installed to /opt/intel...</p>
 */
public class MKL implements BlasLibrary {

  private static final String LIB_PATH = "/opt/intel/mkl/lib/intel64";

  @Override
  public String getConfigureFlags() {
    throw new UnsupportedOperationException("Compiling against MKL not yet supported");
  }

  @Override
  public String getName() {
    return "MKL";
  }

  @Override
  public String getNameAndVersion() {
    return "MKL 2017.2.174";
  }

  @Override
  public String getLibraryPath() {
    return LIB_PATH;
  }

  @Override
  public String getBlasSharedLibraryPath() {
    return LIB_PATH + "/mkl_rt.so";
  }

  @Override
  public String getCompilationId() {
    return "0";
  }

  @Override
  public void ensureInstalled(Node node, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
    FilePath libPath = node.createPath(LIB_PATH);
    FilePath blas = libPath.child("libblas.so.3");
    if(!blas.exists()) {
      throw new RuntimeException("libblas3.so.3 not found at " + blas.getRemote());
    }
    FilePath lapack = libPath.child("liblapack.so.3");
    if(!lapack.exists()) {
      throw new RuntimeException("liblapack.so.3 not found at " + lapack.getRemote());
    }
  }
}
