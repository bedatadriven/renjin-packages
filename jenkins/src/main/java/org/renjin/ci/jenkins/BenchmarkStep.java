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
import org.renjin.ci.jenkins.benchmark.Benchmark;
import org.renjin.ci.jenkins.benchmark.BenchmarkRun;

import java.io.IOException;
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
    
    BenchmarkRun benchmarkRun = new BenchmarkRun(run, workspace, launcher, listener);
    benchmarkRun.setupInterpreter(
        run.getEnvironment(listener).expand(interpreter), 
        run.getEnvironment(listener).expand(interpreterVersion));
    benchmarkRun.start();
    benchmarkRun.run(benchmarks);
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

  @Override
  public Descriptor<Builder> getDescriptor() {
    return new DescriptorImpl();
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