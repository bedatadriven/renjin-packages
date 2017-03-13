package org.renjin.ci.jenkins.benchmark;

import java.security.SecureRandom;


public class CompilationId {

  public static final SecureRandom RANDOM = new SecureRandom();

  public static String generate() {
    return Integer.toHexString(RANDOM.nextInt());
  }
}
