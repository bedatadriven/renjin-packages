package org.renjin.ci.packages;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.tools.mapreduce.MapJob;
import com.google.appengine.tools.pipeline.PipelineServiceFactory;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.PackageTestResult;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Path("/tasks/migrate/tests")
public class TestResources {

  private static final int BATCH_SIZE = 1000;

  @GET
  @Path("/start")
  public void start() {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder
        .withUrl("/tasks/migrate/tests")
        .param("offset", "0"));
  }
  
  @GET
  @Path("/migrateVersions")
  public void migrateVersions() {

    Pipelines.redirectToStatus(Pipelines.applyAll(new MigratePackageVersionKeys()));

  }

  @POST
  public void migrateTests(@FormParam("offset") int offset) throws ClassNotFoundException, SQLException {

    Class.forName("com.mysql.jdbc.GoogleDriver");
    String url = "jdbc:google:mysql://bdd-dev:development/renjin?user=root";


    List<PackageTestResult> toSave = new ArrayList<>();

    try(Connection conn = DriverManager.getConnection(url)) {
      try (Statement statement = conn.createStatement()) {

        try (ResultSet resultSet = statement.executeQuery(
            "SELECT tr.runNumber, t.name, tr.version, r.packageVersion_id pvid, output, errorMessage, r.passed " +
                "FROM TestResult r " +
                "LEFT JOIN TestRun tr on (r.renjinCommitId=tr.commitId) " +
                "LEFT JOIN Test t on (r.test_id=t.id) " +
                "WHERE tr.runNumber=1 " +
                "LIMIT " + offset + "," + BATCH_SIZE)) {

          while (resultSet.next()) {
            PackageVersionId packageVersionId = PackageVersionId.fromTriplet(resultSet.getString("pvid"));
            long runNumber = resultSet.getLong("runNumber");
            String testName = resultSet.getString("name");

            PackageTestResult testResult = new PackageTestResult(packageVersionId, runNumber, testName);
            testResult.setRenjinVersion(resultSet.getString("version"));
            testResult.setPassed(resultSet.getBoolean("passed"));
            testResult.setOutput(resultSet.getString("output"));
            testResult.setError(resultSet.getString("errorMessage"));
            toSave.add(testResult);
          }
        }
      }
    }

    if(!toSave.isEmpty()) {
      ObjectifyService.ofy().save().entities(toSave);

      Queue queue = QueueFactory.getDefaultQueue();

      queue.add(TaskOptions.Builder
          .withUrl("/tasks/migrate/tests")
          .param("offset", Integer.toString(offset + BATCH_SIZE)));
    }

  }
}
