package org.renjin.build.worker;

import java.io.IOException;
import java.net.URI;

public class WebApp {

  private static final URI BASE_URI = URI.create("http://localhost:8080/");

  public static void main(String[] args) throws IOException {
    new PollLoop().run();
  }
}