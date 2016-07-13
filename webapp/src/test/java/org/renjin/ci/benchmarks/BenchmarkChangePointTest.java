package org.renjin.ci.benchmarks;

import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.ScriptExecServlet;
import org.renjin.ci.datastore.BenchmarkResult;
import org.renjin.ci.datastore.PackageDatabase;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;

import javax.script.ScriptException;
import java.io.IOException;

public class BenchmarkChangePointTest extends AbstractDatastoreTest {


  public static final String MACHINE1 = "MAC303A647992E4";

  @Test
  public void test() throws IOException, ScriptException {

    
    PackageDatabase.saveNow(result("GNU R", "3.2.0", MACHINE1, 24000));
    PackageDatabase.saveNow(result("Renjin", "0.8.2130", MACHINE1, 114059));
    PackageDatabase.saveNow(result("Renjin", "0.8.2135", MACHINE1, 63771));
    PackageDatabase.saveNow(result("Renjin", "0.8.2141", MACHINE1, 48137));

    RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
    RenjinScriptEngine scriptEngine = factory.getScriptEngine();
    scriptEngine.put("machineId", MACHINE1);
    scriptEngine.put("benchmarkName", "randomForest");
    
    scriptEngine.eval(ScriptExecServlet.loadScript("analyzeBenchmarks"));

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