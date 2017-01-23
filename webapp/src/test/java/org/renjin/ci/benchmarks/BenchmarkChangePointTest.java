package org.renjin.ci.benchmarks;

import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ScriptExecServlet;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.datastore.BenchmarkSummary;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.eval.EvalException;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;

import javax.script.ScriptException;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class BenchmarkChangePointTest extends AbstractDatastoreTest {


  public static final String MACHINE1 = "MAC303A647992E4";

  @Test
  public void test() throws IOException, ScriptException {

    
    PackageDatabase.saveNow(result("GNU R", "3.2.0", MACHINE1, 24000));
    PackageDatabase.saveNow(result("GNU R", "3.2.0", "FOO", 0));
    PackageDatabase.saveNow(result("Renjin", "0.8.2129", MACHINE1, 114059));
    PackageDatabase.saveNow(result("Renjin", "0.8.2130", MACHINE1, 104044));
    PackageDatabase.saveNow(result("Renjin", "0.8.2135", MACHINE1,  63771));
    PackageDatabase.saveNow(result("Renjin", "0.8.2141", MACHINE1,  48137));
    PackageDatabase.saveNow(result("Renjin", "0.8.2142", MACHINE1,  48240));
    PackageDatabase.saveNow(result("Renjin", "0.8.2143", MACHINE1,  48110));
    PackageDatabase.saveNow(result("Renjin", "0.8.2144", MACHINE1,  48119));
    PackageDatabase.saveNow(result("Renjin", "0.8.2145", MACHINE1,  90400));
    PackageDatabase.saveNow(result("Renjin", "0.8.2146", MACHINE1,  91345));
    PackageDatabase.saveNow(result("Renjin", "0.8.2146", MACHINE1,  91000));

    RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
    RenjinScriptEngine scriptEngine = factory.getScriptEngine();
    scriptEngine.put("machineId", MACHINE1);
    scriptEngine.put("benchmarkName", "randomForest");

    try {
      scriptEngine.eval(ScriptExecServlet.loadScript("analyzeBenchmarks"));

    } catch(EvalException e) {
      e.printRStackTrace(System.out);
      throw e;
    }
    BenchmarkSummary summary = PackageDatabase.getBenchmarkSummaries(MACHINE1).iterator().next();
    assertThat(summary.getRegression(), equalTo("0.8.2145"));

    assertThat(summary.getChangePoints().size(), equalTo(4));
    assertThat(summary.getChangePoints().get(0).getVersion(), equalTo("0.8.2135"));
    assertThat(summary.getChangePoints().get(0).getPreviousVersion(), equalTo("0.8.2130"));
    assertThat(summary.getChangePoints().get(0).getMean(), equalTo(55954.0));
    assertThat(summary.getChangePoints().get(0).getPreviousMean(), equalTo(109051.5));
    assertThat(summary.getChangePoints().get(0).getPercentageChangeString(), equalTo("49%"));


    assertThat(summary.getChangePoints().get(3).getVersion(), equalTo("0.8.2145"));
  }

  private BenchmarkResult result(String interpreter, String interpreterVersion, String machineId, long runTime) {
    BenchmarkResult result = new BenchmarkResult();
    result.setInterpreter(interpreter);
    result.setInterpreterVersion(interpreterVersion);
    result.setBenchmarkName("randomForest");
    result.setMachineId(machineId);
    result.setCompleted(true);
    result.setRunTime(runTime);
    return result;
  }

}