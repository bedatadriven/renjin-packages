package org.renjin.ci.packages;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import org.joda.time.LocalDateTime;
import org.renjin.ci.datastore.PackageTestResult;
import org.renjin.ci.datastore.RenjinCommit;
import org.renjin.ci.datastore.RenjinRelease;
import org.renjin.ci.model.PackageVersionId;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Path("/tasks/migrate/tests")
public class TestResources {

  private static final Logger LOGGER = Logger.getLogger(TestResources.class.getName());
  
  private static final int BATCH_SIZE = 1000;

  @GET
  @Path("/start")
  public Response start() {
    Queue queue = QueueFactory.getDefaultQueue();
    queue.add(TaskOptions.Builder
        .withUrl("/tasks/migrate/tests")
        .param("offset", "0"));
    
    return Response.ok().build();
  }
  
  @POST
  public void migrateTests(@FormParam("offset") int offset) throws ClassNotFoundException, SQLException {

    Class.forName("com.mysql.jdbc.GoogleDriver");
    String url = "jdbc:google:mysql://bdd-dev:development/renjin?user=root";

    long runNumber = 6;
    String renjinSha1 = "412d8b64b20eef7cdab7fec380d0bc1bbb679edc";
    String renjinVersion = "0.7.0-RC7-SNAPSHOT-20131015";
    Date renjinDate = new LocalDateTime(2013, 10, 15, 17, 18, 52).toDate();
    
    if(offset == 0) {

      RenjinRelease release = new RenjinRelease();
      release.setDate(renjinDate);
      release.setRenjinCommit(com.googlecode.objectify.Ref.create(Key.create(RenjinCommit.class, renjinSha1)));
      release.setVersion(renjinVersion);

      RenjinCommit commit = ObjectifyService.ofy().load().key(Key.create(RenjinCommit.class, renjinSha1)).now();
      commit.setRelease(Ref.create(release));

//    
//      PackageTestRun run = new PackageTestRun(runNumber);
//      run.setRenjinVersion(renjinVersion);
//      run.setTestDate(renjinDate);
//
//      ObjectifyService.ofy().save().entities(release, commit, run);
    }
    
    List<PackageTestResult> toSave = new ArrayList<>();

    try(Connection conn = DriverManager.getConnection(url)) {
      try (Statement statement = conn.createStatement()) {

        try (ResultSet resultSet = statement.executeQuery(
            "SELECT  t.name, c.version, b.id buildNumber, r.packageVersion_id pvid, output, errorMessage, r.passed " +
                "FROM TestResult r " +
                "LEFT JOIN Build b on (b.started = r.startTime) " +
                "LEFT JOIN RenjinCommit c on (b.renjinCommitId = c.id) " +
                "LEFT JOIN Test t on (r.test_id=t.id) " +
                "WHERE tr.runNumber=" + runNumber + " " +
                "LIMIT " + offset + "," + BATCH_SIZE)) {

          while (resultSet.next()) {
            PackageVersionId packageVersionId = PackageVersionId.fromTriplet(resultSet.getString("pvid"));
            String testName = resultSet.getString("name");

//            PackageTestResult testResult = new PackageTestResult(packageVersionId, runNumber, testName);
//            testResult.setRenjinVersion(resultSet.getString("version"));
//            testResult.setPassed(resultSet.getBoolean("passed"));
//            testResult.setPackageBuildNumber(18);
//            testResult.setOutput(resultSet.getString("output"));
//            testResult.setError(resultSet.getString("errorMessage"));
//            toSave.add(testResult);
          }
        }
      }
    }

    if(!toSave.isEmpty()) {
      ObjectifyService.ofy().save().entities(toSave);
      
      LOGGER.info("Saved " + toSave.size() + " results");

      Queue queue = QueueFactory.getDefaultQueue();

      queue.add(TaskOptions.Builder
          .withUrl("/tasks/migrate/tests")
          .param("offset", Integer.toString(offset + BATCH_SIZE)));
    }

  }
}
