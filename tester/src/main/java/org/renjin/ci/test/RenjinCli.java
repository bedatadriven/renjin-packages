package org.renjin.ci.test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.toArray;

/**
 * Wrapper for running the org.renjin.cli.Main class via Reflection
 */
public class RenjinCli {

  private final Method mainMethod;

  public RenjinCli(URL[] dependencies) throws Exception {
    URLClassLoader classLoader = new URLClassLoader(dependencies);
    Class<?> mainClass = classLoader.loadClass("org.renjin.cli.Main");
    mainMethod = mainClass.getMethod("main", String[].class);
  }

  public RenjinCli(List<URL> renjinDependencies, List<URL> packageDependencies) throws Exception {
    this(toArray(concat(renjinDependencies, packageDependencies), URL.class));
  }

  public void run(String source) throws Exception {
    String[] args = new String[] { "-e", source};
    mainMethod.invoke(null, new Object[] { args });
  }
}
