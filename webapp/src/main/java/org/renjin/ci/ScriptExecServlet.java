package org.renjin.ci;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.renjin.appengine.AppEngineContextFactory;
import org.renjin.eval.EvalException;
import org.renjin.sexp.StringArrayVector;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes R scripts as tasks
 */
public class ScriptExecServlet extends HttpServlet {

  public static final String SCRIPT_NAME_PARAMETER = "script";

  private static final Logger LOGGER = Logger.getLogger(ScriptExecServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    
    // Only allow execution via task queue
    if(Strings.isNullOrEmpty(req.getHeader("X-AppEngine-QueueName"))) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
 
    String script;
    try {
      script = loadScript(req.getParameter(SCRIPT_NAME_PARAMETER));
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to load script", e);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    ScriptEngine scriptEngine = AppEngineContextFactory.createScriptEngine(getServletContext());
    Enumeration names = req.getParameterNames();
    while(names.hasMoreElements()) {
      String parameterName = (String) names.nextElement();
      if(!parameterName.equals(SCRIPT_NAME_PARAMETER)) {
        String[] parameterValue = req.getParameterValues(parameterName);
        scriptEngine.put(parameterName, new StringArrayVector(parameterValue));
      }
    }

    try {
      scriptEngine.eval(script);
    } catch (EvalException e) {
      LOGGER.severe("ERROR: " + e.getMessage());
      e.printRStackTrace(System.err);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
      
    } catch (ScriptException e) {
      LOGGER.log(Level.SEVERE, "Exception executing script", e);
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }
  }

  public static String loadScript(String scriptName) throws IOException {

    if(Strings.isNullOrEmpty(scriptName)) {
      throw new IllegalArgumentException("Parameter '" + SCRIPT_NAME_PARAMETER + "' is missing");
    }

    return Resources.toString(Resources.getResource(scriptName + ".R"), Charsets.UTF_8);
  }
}
