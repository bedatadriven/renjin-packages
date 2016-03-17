package org.renjin.ci.jenkins;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
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
import org.renjin.ci.jenkins.benchmark.BenchmarkDataset;
import org.renjin.ci.jenkins.benchmark.Interpreter;
import org.renjin.ci.jenkins.benchmark.Renjin;

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

    Interpreter interpreter = setupInterpreter(workspace, launcher, listener);

    for (Benchmark benchmark : benchmarks) {
      if(accept(benchmark)) {
        run(launcher, interpreter, benchmark, listener);
      }
    }
    
  }

  private Interpreter setupInterpreter(FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    Renjin renjin = new Renjin(workspace, listener, interpreterVersion);
    renjin.ensureInstalled();
    
    return renjin;
  }
  
  private void findBenchmarks(List<Benchmark> benchmarks, String namePrefix, FilePath parentDir, TaskListener listener) throws IOException, InterruptedException {
    for (FilePath childDir : parentDir.listDirectories()) {
      if (childDir.child("BENCHMARK.dcf").exists()) {
        benchmarks.add(new Benchmark(namePrefix + childDir.getName(), childDir));
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

  private void run(Launcher launcher, Interpreter interpreter, Benchmark benchmark, TaskListener listener) throws IOException, InterruptedException {

    List<BenchmarkDataset> datasets = benchmark.readDatasets();
    for (BenchmarkDataset dataset : datasets) {
      FilePath datasetPath = benchmark.getDirectory().child(dataset.getFileName());
      if(!datasetPath.exists()) {
        listener.getLogger().println("Downloading " + dataset.getFileName() + " from " + dataset.getUrl());
        datasetPath.copyFrom(dataset.getUrl());
      }
    }
    
    String runScript = String.format("system.time(result <- source('%s', local = new.env()));\n", benchmark.getScript().getName());
    
    FilePath runScriptFile = benchmark.getDirectory().child("harness.R");
    runScriptFile.write(runScript, Charsets.UTF_8.name());
    
    interpreter.execute(launcher, runScriptFile);
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