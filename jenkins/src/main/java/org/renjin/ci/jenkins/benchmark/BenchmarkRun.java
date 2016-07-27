package org.renjin.ci.jenkins.benchmark;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import org.renjin.ci.RenjinCiClient;
import org.renjin.ci.model.BenchmarkRunDescriptor;

import java.io.IOException;
import java.net.URL;
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

  public void setupInterpreter(String interpreter, String version, BlasLibrary blasLibrary, JDK jdk) throws IOException, InterruptedException {
    if("GNU R".equalsIgnoreCase(interpreter)) {
      this.interpreter = new GnuR(version, blasLibrary);
    } else if("pqR".equalsIgnoreCase(interpreter)) {
      this.interpreter = new PQR(version);
    } else if("TERR".equalsIgnoreCase(interpreter)) {
      this.interpreter = new Terr(version);
    } else {
      this.interpreter = new Renjin(jdk, version);
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
    benchmarkRun.setRunVariables(interpreter.getRunVariables());
    
    try {
      benchmarkRun.setMachine(launcher.getChannel().call(new UnixDescriber()));
    } catch (IOException e) {
      throw new AbortException("Error getting machine description");
    }

    runId = RenjinCiClient.startBenchmarkRun(benchmarkRun);

    listener.getLogger().println("Starting benchmark run #" + runId + "...");

    
  }
  
  public boolean run(List<Benchmark> benchmarks, boolean dryRun) throws IOException, InterruptedException {
    boolean allPassed = true;
    
    for (Benchmark benchmark : benchmarks) {
      try {
        run(benchmark, dryRun);
      } catch (InterruptedException e) {
        throw e;
      } catch(Exception e) {
        allPassed = false;
        listener.error("Failed to run benchmark: " + benchmark.getName());
        e.printStackTrace(listener.getLogger());
      }
    }
    
    return allPassed;
  }

  private void run(Benchmark benchmark, boolean dryRun) throws IOException, InterruptedException {

    listener.getLogger().println("Running benchmark " + benchmark.getName() + "...");

    if(!dryRun) {
      ensureDatasetsDownloaded(benchmark);
    }
    
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

    boolean completed = interpreter.execute(launcher, listener, node, runScriptFile, benchmark.getDependencies(), dryRun, -1);

    if(!dryRun) {
      if (completed && timingFile.exists()) {
        double milliseconds = parseTiming(timingFile);

        RenjinCiClient.postBenchmarkResult(runId, benchmark.getName(), (long) milliseconds);

      } else {
        RenjinCiClient.postBenchmarkFailure(runId, benchmark.getName());
      }
    }
  }

  private double parseTiming(FilePath timingFile) throws IOException, InterruptedException {
    String timing = timingFile.readToString();
    
    // Early versions of Renjin included a comma in the output!
    timing = timing.replace(",", "");
    
    return Double.parseDouble(timing);
  }
  

  private void ensureDatasetsDownloaded(Benchmark benchmark) throws IOException, InterruptedException {
    
    FilePath sharedDataDir = node.getRootPath().child("data");
    
    List<BenchmarkDataset> datasets = benchmark.getDatasets();
    for (BenchmarkDataset dataset : datasets) {
      
      if(Strings.isNullOrEmpty(dataset.getHash())) {
        throw new RuntimeException("Dataset in " + benchmark.getName() + "/" + dataset.getFileName() + " is missing MD5 hash");
      }
      
      // If the file already exists, verify its hash,
      // and remove if it's incorrect
      FilePath datasetPath = benchmark.getDirectory().child(dataset.getFileName());
      if(datasetPath.exists()) {
        if(datasetPath.digest().equals(dataset.getHash())) {
          continue;
        }
        datasetPath.delete();
      }

      // Download the file to a shared data location on the node
      FilePath sharedFile = sharedDataDir.child(dataset.getHash());
      if(!sharedFile.exists()) {
        listener.getLogger().println("Downloading dataset " + dataset.getFileName());

        FilePath tempFile = sharedDataDir.child(dataset.getHash() + ".download");
        tempFile.copyFrom(new URL(dataset.getUrl()));
        if(!tempFile.digest().equals(dataset.getHash())) {
          throw new RuntimeException("Dataset " + benchmark.getName() + "/" + dataset.getFileName() + ", downloaded from " + 
              dataset.getUrl() + " has incorrect hash.");
        }
        tempFile.renameTo(sharedFile);
      }

      // Symlink the dataset into the workspace to avoid too much extra space used
      listener.getLogger().println("Linking dataset " + dataset.getFileName());
      datasetPath.symlinkTo(sharedFile.getRemote(), listener);
    }
  }

  private String composeHarnessScript(Benchmark benchmark) {
    return String.format("timing <- system.time(result <- source('%s', local = new.env()));\n" +
        "cat(timing[[3]]*1000, file='timing.out');\n"
        , benchmark.getScript().getName());
  }

}
