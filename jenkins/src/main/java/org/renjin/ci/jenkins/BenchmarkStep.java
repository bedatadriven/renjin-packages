package org.renjin.ci.jenkins;

import com.google.common.collect.Lists;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.jenkins.benchmark.Benchmark;
import org.renjin.ci.jenkins.benchmark.BenchmarkRun;
import org.renjin.ci.model.RenjinVersionId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BenchmarkStep extends Builder  implements SimpleBuildStep {
  
  private String interpreter;
  private String interpreterVersion;
  private String includes;

  @DataBoundConstructor
  public BenchmarkStep(String interpreter, String interpreterVersion, String includes) {
    this.interpreter = interpreter;
    this.interpreterVersion = interpreterVersion;
    this.includes = includes;
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

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
    List<Benchmark> benchmarks = Lists.newArrayList();
    findBenchmarks(benchmarks, "", workspace, listener);
    
    if(benchmarks.isEmpty()) {
      listener.getLogger().println("No benchmarks found.");
      throw new AbortException();
    }

    String interpreter = run.getEnvironment(listener).expand(this.interpreter);
    List<String> interpreterVersions = expandVersions(interpreter, run, listener);

    for (String interpreterVersion : interpreterVersions) {
      try {
        BenchmarkRun benchmarkRun = new BenchmarkRun(run, workspace, launcher, listener);
        benchmarkRun.setupInterpreter(
            interpreter,
            interpreterVersion);
        benchmarkRun.start();
        benchmarkRun.run(benchmarks);
      
      } catch (InterruptedException e) {
        listener.error("Interrupted. Stopping.");
        return;
        
      } catch (Exception e) {
        listener.error("Failed to run benchmarks on " + interpreter + " " + interpreterVersion + ": " + 
          e.getMessage());
        e.printStackTrace(listener.getLogger());
      }
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

  private void findBenchmarks(List<Benchmark> benchmarks, String namePrefix, FilePath parentDir, TaskListener listener) throws IOException, InterruptedException {
    for (FilePath childDir : parentDir.listDirectories()) {
      if (childDir.child("BENCHMARK.dcf").exists() || 
          childDir.child("BENCHMARK").exists()) {

        Benchmark benchmark = new Benchmark(namePrefix + childDir.getName(), childDir);
        if(accept(benchmark)) {
          benchmarks.add(benchmark);
        }
      } else {
        findBenchmarks(benchmarks, namePrefix + childDir.getName() + "/", childDir, listener);
      }
    }
  }

  private boolean accept(Benchmark benchmark) {
    if(includes == null) {
      return true;
    } else {
      return benchmark.getName().contains(includes);
    }
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