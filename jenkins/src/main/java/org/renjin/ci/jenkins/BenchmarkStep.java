package org.renjin.ci.jenkins;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.jenkins.benchmark.*;
import org.renjin.ci.model.RenjinVersionId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BenchmarkStep extends Builder  implements SimpleBuildStep {
  
  private String interpreter;
  private String interpreterVersion;
  private String includes;
  private String excludes;
  private String blas;
  private String jdk;
  private boolean dryRun;
  private boolean noDependencies;

  @DataBoundConstructor
  public BenchmarkStep(String interpreter, String interpreterVersion, String includes,
                       String excludes, String blas, String jdk, boolean dryRun, boolean noDependencies) {
    this.interpreter = interpreter;
    this.interpreterVersion = interpreterVersion;
    this.includes = includes;
    this.excludes = excludes;
    this.blas = blas;
    this.jdk = jdk;
    this.dryRun = dryRun;
    this.noDependencies = noDependencies;
  }

  public String getInterpreterVersion() {
    return interpreterVersion;
  }

  public String getIncludes() {
    return includes;
  }

  public String getInterpreter() {
    return interpreter;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public String getExcludes() {
    return excludes;
  }

  public boolean isNoDependencies() {
    return noDependencies;
  }

  public String getBlas() {
    return blas;
  }

  public String getJdk() {
    return jdk;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    List<Benchmark> benchmarks = Lists.newArrayList();
    findBenchmarks(benchmarks, "", workspace);
    
    if(benchmarks.isEmpty()) {
      listener.getLogger().println("No benchmarks found.");
      throw new AbortException();
    }
    
    JDK jdk = findJdk()
        .forNode(((AbstractBuild) run).getBuiltOn(), listener)
        .forEnvironment(run.getEnvironment(listener));
    
    String interpreter = run.getEnvironment(listener).expand(this.interpreter);
    List<String> interpreterVersions = expandVersions(interpreter, run, listener);

    boolean allPassed = true;
    
    for (String interpreterVersion : interpreterVersions) {
      try {
        BenchmarkRun benchmarkRun = new BenchmarkRun(run, workspace, launcher, listener);
        benchmarkRun.setupInterpreter(
            interpreter,
            interpreterVersion,
            blasLibrary(), 
            jdk);
        benchmarkRun.start();
        if(!benchmarkRun.run(benchmarks, dryRun)) {
          allPassed = false;
        }
      
      } catch (InterruptedException e) {
        listener.error("Interrupted. Stopping.");
        return;
        
      } catch (Exception e) {
        listener.error("Failed to run benchmarks on " + interpreter + " " + interpreterVersion + ": " + 
          e.getMessage());
        e.printStackTrace(listener.getLogger());
        return;
      }
    }
    
    if(!allPassed) {
      throw new AbortException("There were failures executing benchmarks.");
    }
  }

  private JDK findJdk() throws AbortException {

    List<JDK> tools = Jenkins.getInstance().getJDKs();
    if(tools.isEmpty()) {
      throw new AbortException("No JDKs setup.");
    }
    
    if(Strings.isNullOrEmpty(jdk)) {
      return tools.get(0);
    }
    
    for (JDK tool : Jenkins.getInstance().getJDKs()) {
      if(tool.getName().equals(jdk)) {
        return tool;
      }
    }
    throw new ConfigException("Couldn't find JDK named '" + jdk + "'");
  }

  private BlasLibrary blasLibrary() {
    if("OpenBLAS".equals(blas)) {
      return new OpenBlas();
    } else {
      return new DefaultBlas();
    }
  }

  private List<String> expandVersions(String interpreter, Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
    String range = run.getEnvironment(listener).expand(this.interpreterVersion);
    if(interpreter.equals("Renjin")) {
      return expandRenjinVersions(range);

    } else {
      return Collections.singletonList(range);
    }    
  }

  static List<String> expandRenjinVersions(String range) {
    if(range.endsWith(",")) {
      return queryVersions(range.substring(0, range.length()-1), null);
    }
    return Collections.singletonList(range);
  }

  private static List<String> queryVersions(String from, String to) {
    List<String> versionStrings = new ArrayList<String>();
    for (RenjinVersionId renjinVersionId : RenjinCiClient.getRenjinVersionRange(from, to)) {
      versionStrings.add(renjinVersionId.toString());
    }
    return versionStrings;
  }

  private void findBenchmarks(List<Benchmark> benchmarks, String namePrefix, FilePath parentDir) throws IOException, InterruptedException {
    for (FilePath childDir : parentDir.listDirectories()) {
      if (childDir.child("BENCHMARK.dcf").exists() || 
          childDir.child("BENCHMARK").exists()) {

        Benchmark benchmark = Benchmark.read(namePrefix, childDir);
        if(accept(benchmark)) {
          benchmarks.add(benchmark);
        }
      } else {
        findBenchmarks(benchmarks, namePrefix + childDir.getName() + "/", childDir);
      }
    }
  }

  private boolean accept(Benchmark benchmark) {
    if(!Strings.isNullOrEmpty(includes) && !benchmark.getName().contains(includes)) {
      return false;
    }
    if(!Strings.isNullOrEmpty(excludes) && benchmark.getName().contains(excludes)) {
      return false;
    }
    if(noDependencies && benchmark.hasDependencies()) {
      return false;
    }
    return true;
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<Builder> {
    public DescriptorImpl() {
    }

    public String getDisplayName() {
      return "Run Renjin Benchmarks";
    }
  }

}