package org.renjin.ci.jenkins.benchmark;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.BenchmarkRunDescriptor;

import java.io.IOException;
import java.util.List;

/**
 * Encapsulates all the state associated with a single run of one or more benchmarks
 */
public class BenchmarkRun {

  private final EnvVars env;
  private final FilePath workspace;
  private final Launcher launcher;
  private final TaskListener listener;
  
  
  private Interpreter interpreter;
  private long runId;
  private final Node node;

  public BenchmarkRun(Run build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
    this.workspace = workspace;
    this.launcher = launcher;
    this.listener = listener;
    env = build.getEnvironment(listener);
    node = ((AbstractBuild) build).getBuiltOn();
  }

  public void setupInterpreter(String interpreter, String version) throws IOException, InterruptedException {
    if("GNU R".equals(interpreter)) {
      this.interpreter = new GnuR(version);
    } else {
      this.interpreter = new Renjin(workspace, listener, version);
    }
    
    this.interpreter.ensureInstalled(node, launcher, listener);
  }
  
  public void start() throws AbortException, InterruptedException {
    Preconditions.checkState(interpreter != null);
    
    BenchmarkRunDescriptor benchmarkRun = new BenchmarkRunDescriptor();
    benchmarkRun.setInterpreter(interpreter.getId());
    benchmarkRun.setInterpreterVersion(interpreter.getVersion());
    benchmarkRun.setRepoUrl(env.get("GIT_URL"));
    benchmarkRun.setCommitId(env.get("GIT_COMMIT"));
    try {
      benchmarkRun.setMachine(launcher.getChannel().call(new UnixDescriber()));
    } catch (IOException e) {
      throw new AbortException("Error getting machine description");
    }

    runId = RenjinCiClient.startBenchmarkRun(benchmarkRun);

    listener.getLogger().println("Starting benchmark run #" + runId + "...");

    
  }
  
  public void run(List<Benchmark> benchmarks) throws IOException, InterruptedException {
    for (Benchmark benchmark : benchmarks) {
      run(benchmark);
    }
  }

  private void run(Benchmark benchmark) throws IOException, InterruptedException {

    listener.getLogger().println("Running benchmark " + benchmark.getName() + "...");

    ensureDatasetsDownloaded(benchmark);

    // Write out a simple script that will source the benchmark
    // and write the timing to an output file
    // In this way, we exclude the interpreter start up time from the benchmark
    String runScript = composeHarnessScript(benchmark);

    // Ensure that the timing file is cleaned up from previous runs
    FilePath timingFile = benchmark.getDirectory().child("timing.out");
    if(timingFile.exists()) {
      timingFile.delete();
    }

    FilePath runScriptFile = benchmark.getDirectory().child("harness.R");
    runScriptFile.write(runScript, Charsets.UTF_8.name());

    boolean completed = interpreter.execute(launcher, listener, runScriptFile);

    if(completed && timingFile.exists()) {
      double milliseconds = parseTiming(timingFile);
      
      RenjinCiClient.postBenchmarkResult(runId, benchmark.getName(), (long)milliseconds);
    
    } else {
      RenjinCiClient.postBenchmarkFailure(runId, benchmark.getName());
    }
  }

  private double parseTiming(FilePath timingFile) throws IOException, InterruptedException {
    String timing = timingFile.readToString();
    
    // Early versions of Renjin included a comma in the output!
    timing = timing.replace(",", "");
    
    return Double.parseDouble(timing);
  }
  

  private void ensureDatasetsDownloaded(Benchmark benchmark) throws IOException, InterruptedException {
    List<BenchmarkDataset> datasets = benchmark.readDatasets();
    for (BenchmarkDataset dataset : datasets) {
      FilePath datasetPath = benchmark.getDirectory().child(dataset.getFileName());
      if(!datasetPath.exists()) {
        listener.getLogger().println("Downloading " + dataset.getFileName() + " from " + dataset.getUrl());
        datasetPath.copyFrom(dataset.getUrl());
      }
    }
  }

  private String composeHarnessScript(Benchmark benchmark) {
    return String.format("timing <- system.time(result <- source('%s', local = new.env()));\n" +
        "cat(timing[[3]]*1000, file='timing.out');\n"
        , benchmark.getScript().getName());
  }

}