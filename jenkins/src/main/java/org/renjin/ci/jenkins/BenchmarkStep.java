package org.renjin.ci.jenkins;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Predicate;
import com.google.common.io.CharStreams;
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class BenchmarkStep extends Builder implements SimpleBuildStep {

  private static final Pattern RUN_CONVENTION_PATTERN = Pattern.compile("^run\\s*<-\\s*function\\(.*");

  private String interpreter;
  private String interpreterVersion;
  private String includes;
  private String excludes;
  private String blas;
  private String jdk;
  private boolean dryRun;
  private boolean noDependencies;
  private Double timeoutMinutes;
  private String namespace;
  private int executions = 1;

  @DataBoundConstructor
  public BenchmarkStep(String interpreter, String interpreterVersion, String includes,
                       String excludes, String blas, String jdk, boolean dryRun, boolean noDependencies,
                       Double timeoutMinutes, String namespace, int executions) {
    this.interpreter = interpreter;
    this.interpreterVersion = interpreterVersion;
    this.includes = includes;
    this.excludes = excludes;
    this.blas = blas;
    this.jdk = jdk;
    this.dryRun = dryRun;
    this.noDependencies = noDependencies;
    this.timeoutMinutes = timeoutMinutes;
    this.namespace = namespace;
    this.executions = executions;
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

  public Double getTimeoutMinutes() {
    return timeoutMinutes;
  }

  public String getNamespace() {
    return namespace;
  }

  public int getExecutions() {
    return executions;
  }

  @Override
  public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

    String prefix = "";
    if(!Strings.isNullOrEmpty(namespace)) {
      prefix = namespace;
      if(!prefix.endsWith("/")) {
        prefix = prefix + "/";
      }
    }

    Filter filter = new Filter(
        run.getEnvironment(listener).expand(includes),
        run.getEnvironment(listener).expand(excludes), noDependencies);

    List<Benchmark> benchmarks = findBenchmarks(filter, prefix, workspace);

    if(benchmarks.isEmpty()) {
      listener.getLogger().println("No benchmarks found.");
      throw new AbortException();
    }

    JDK jdk = findJdk()
        .forNode(((AbstractBuild) run).getBuiltOn(), listener)
        .forEnvironment(run.getEnvironment(listener));
    
    String interpreter = run.getEnvironment(listener).expand(this.interpreter);
    List<String> interpreterVersions = expandVersions(interpreter, run, listener);

    // Randomize order of versions and benchmarks to avoid
    // run-order effects.
    Collections.shuffle(interpreterVersions);

    benchmarks = repeat(benchmarks);
    Collections.shuffle(benchmarks);

    boolean allPassed = true;
    
    for (String interpreterVersion : interpreterVersions) {
      try {
        BenchmarkRun benchmarkRun = new BenchmarkRun(run, workspace, launcher, listener);
        benchmarkRun.setTimeoutMinutes(timeoutMinutes);
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

  private List<Benchmark> repeat(List<Benchmark> benchmarks) {
    List<Benchmark> schedule = new ArrayList<Benchmark>();
    for (int i = 0; i < executions; i++) {
      schedule.addAll(benchmarks);
    }
    return schedule;
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
    } else if("f2jblas".equalsIgnoreCase(blas)) {
      return new F2JBlas();
    } else if("mkl".equalsIgnoreCase(blas)) {
      return new MKL();
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

  private List<Benchmark> findBenchmarks(Filter filter, String prefix, FilePath parentDir) throws IOException, InterruptedException {
    List<Benchmark> list = new ArrayList<Benchmark>();
    findBenchmarks(filter, list, prefix, parentDir);
    return list;
  }

  private void findBenchmarks(Filter filter, List<Benchmark> benchmarks, String namePrefix, FilePath parentDir) throws IOException, InterruptedException {
    for (FilePath childDir : parentDir.listDirectories()) {
      if (childDir.child("BENCHMARK.dcf").exists() || 
          childDir.child("BENCHMARK").exists()) {

        Benchmark benchmark = Benchmark.fromBenchmarkDir(namePrefix, childDir);
        if (filter.apply(benchmark)) {
          benchmarks.add(benchmark);
        }
      } else {

        // Look for benchmark .lists
        for (FilePath filePath : childDir.list()) {
          if(!filePath.isDirectory() && filePath.getName().endsWith(".list")) {
            readBenchmarksList(filter, benchmarks, namePrefix + childDir.getName() + "/", filePath);
          }
        }

        findBenchmarks(filter, benchmarks, namePrefix + childDir.getName() + "/", childDir);
      }
    }
  }

  private void readBenchmarksList(Predicate<Benchmark> filter, List<Benchmark> benchmarks, String prefix, FilePath listFile) throws IOException, InterruptedException {
    List<String> lines = CharStreams.readLines(new StringReader(listFile.readToString()));
    for (String line : lines) {

      // Remove comments
      int commentStart = line.indexOf('#');
      if(commentStart != -1) {
        line = line.substring(0, commentStart);
      }

      // Remove extra whitespace
      line = line.trim();
      if(!line.isEmpty()) {

        String[] columns = line.split("\\s+");
        if(columns.length >= 1) {
          FilePath scriptPath =  listFile.getParent().child(columns[0]);
          if(scriptPath.exists()) {
            Benchmark benchmark = new Benchmark(BenchmarkConvention.RBENCH, composeName(prefix, columns[0]), scriptPath);
            if(filter.apply(benchmark)) {
              benchmarks.add(benchmark);
            }
          }
        }
      }
    }
  }

  private String composeName(String prefix, String scriptName) {
    if(scriptName.endsWith(".R")) {
      scriptName = scriptName.substring(0, scriptName.length() - ".R".length());
    }
    String parts[] = scriptName.split("/");
    while(parts.length >= 2) {
      // In the case of "benchmark/benchmark.R", keep only "benchmark" as a name
      int lastPart = parts.length - 1;
      if (parts[lastPart].equals(parts[lastPart - 1])) {
        parts = Arrays.copyOf(parts, parts.length - 1);
      } else {
        break;
      }
    }

    return prefix + Joiner.on('/').join(parts);
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