package org.renjin.build.model;

/**
 * Indicates a problem with the package that cannot be resolved.
 */
public class InvalidPackageException extends Exception {
  public InvalidPackageException(String message) {
    super(message);
  }
}
