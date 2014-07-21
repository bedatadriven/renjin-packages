package org.renjin.ci.tester;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.eclipse.aether.artifact.DefaultArtifact;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
  private static Logger LOGGER = Logger.getLogger(Main.class.getName()).getParent();

  public static void main(String args[]) throws Exception {
    if (args.length != 3) {
      System.err.printf("Please supply exactly three arguments:\n1.\t%s\n2.\t%s\n3.\t%s\n",
          "The Renjin version",
          "The coordinates of the packages under test",
          "The path to the test to be run");
      System.exit(-1);
    }

    String renjinVersion = args[0];
    String packageUnderTestCoordinates = args[1];
    String testPath = args[2];

    File testFile = new File(testPath);
    Preconditions.checkState(testFile.exists());

    DefaultArtifact packageArtifact = new DefaultArtifact(packageUnderTestCoordinates);
    String packageUnderTest = packageArtifact.getArtifactId();

    List<URL> classPathURLs = Lists.newArrayList();
    DependencyResolver resolver = new DependencyResolver();
    classPathURLs.addAll(resolver.resolveRenjin(renjinVersion));
    classPathURLs.addAll(resolver.resolvePackage(packageArtifact));

//    System.out.println(Joiner.on("\n").join(classPathURLs));

    ClassLoader classLoader = createClassLoader(classPathURLs);
    ScriptEngineFactory factory = (ScriptEngineFactory) classLoader.loadClass("org.renjin.script.RenjinScriptEngineFactory").newInstance();
    ScriptEngine engine = factory.getScriptEngine();

    // load package
    engine.eval(String.format("library(%s)", packageUnderTest));

    // run test
    try(FileReader testReader = new FileReader(testFile)) {
      engine.eval(new FileReader(testPath));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "An exception occurred while running the requested test.", e);
    }
  }

  private static ClassLoader createClassLoader(List<URL> classpathURLs) throws MalformedURLException {
    URL[] urls = classpathURLs.toArray(new URL[classpathURLs.size()]);
    ClassLoader bootstrapLoader = ClassLoader.getSystemClassLoader().getParent();
    return new URLClassLoader(urls, bootstrapLoader);
  }
}
