package org.renjin.ci.benchmarks;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Charsets;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.datastore.*;
import org.renjin.ci.model.BenchmarkRunDescriptor;
import org.renjin.ci.model.MachineDescriptor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Path("/benchmarks")
public class BenchmarksResource {

  private static final Logger LOGGER = Logger.getLogger(BenchmarksResource.class.getName());
  public static final int MINIMUM_HARNESS_VERSION = 4;

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Viewable getIndex() {

    Map<String, Object> model = new HashMap<>();
    model.put("page", new BenchmarkPage());
    
    return new Viewable("/benchmarks.ftl", model);
  }
  
  @GET
  @Path("machine/{machineId}")
  @Produces(MediaType.TEXT_HTML) 
  public Viewable getMachineResults(@PathParam("machineId") String machineId) {
    Map<String, Object> model = new HashMap<>();
    model.put("page", new MachinePage(machineId));
    
    return new Viewable("/benchmarkMachine.ftl", model);
  }

  @GET
  @Path("machine/{machineId}/benchmark/{benchmarkName:.+}")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getMachineBenchmarkResultsPage(@PathParam("machineId") String machineId, 
                                                 @PathParam("benchmarkName") String benchmarkName) {

    DetailPage page = new DetailPage(machineId, benchmarkName);
    
    Map<String, Object> model = new HashMap<>();
    model.put("page", page);

    return new Viewable("/benchmarkResults.ftl", model);
  }


  @GET
  @Path("machine/{machineId}/benchmark/{benchmarkName:.+}.csv")
  @Produces("text/csv")
  public StreamingOutput getMachineBenchmarkResults(@PathParam("machineId") String machineId,
                                                    @PathParam("benchmarkName") String benchmarkName) {

    final QueryResultIterator<BenchmarkResult> it = PackageDatabase.getBenchmarkResultsForMachine(machineId, benchmarkName).iterator();
    return new StreamingOutput() {

      @Override
      public void write(OutputStream outputStream) throws IOException, WebApplicationException {
        PrintWriter csv = new PrintWriter(new OutputStreamWriter(outputStream, Charsets.UTF_8));
        csv.println("machine,interpreter,interpreter.version,jdk,blas,time");
        while(it.hasNext()) {
          BenchmarkResult result = it.next();
          if(result.isCompleted() && result.getHarnessVersion() >= MINIMUM_HARNESS_VERSION) {
            csv.print(result.getMachineId());
            csv.print(",");
            csv.print(result.getInterpreter());
            csv.print(",");
            csv.print(result.getInterpreterVersion());
            csv.print(",");
            csv.print(result.getRunVariable("JDK"));
            csv.print(",");
            csv.print(result.getRunVariable("BLAS"));
            csv.print(",");
            csv.print(result.getRunTime());
            csv.println();
          }
        }
        csv.flush();
      }
    };
  }


  private void queueSummarize(String machineId, String benchmarkName) {
    QueueFactory.getDefaultQueue().add(TaskOptions.Builder.withUrl("/_script")
      .param("script", "analyzeBenchmarks")
      .param("machineId", machineId)
      .param("benchmarkName", benchmarkName)
        .retryOptions(RetryOptions.Builder.withTaskRetryLimit(3)));
  }
  
  @POST
  @Path("runs")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.TEXT_PLAIN)
  public String post(final BenchmarkRunDescriptor descriptor) {

    LOGGER.info("Received update from machine " + descriptor.getMachine().getId());
    
    createMachineIfNotExists(descriptor.getMachine());

    BenchmarkRun run = new BenchmarkRun();
    run.setId(nextRunNumber());
    run.setHarnessVersion(descriptor.getHarnessVersion());
    run.setMachineId(descriptor.getMachine().getId());
    run.setRepoUrl(descriptor.getRepoUrl());
    run.setCommitId(descriptor.getCommitId());
    run.setStartTime(new Date());
    run.setInterpreter(descriptor.getInterpreter());
    run.setInterpreterVersion(descriptor.getInterpreterVersion());
    run.setRunVariables(descriptor.getRunVariables());

    Key<BenchmarkRun> key = ObjectifyService.ofy().save().entity(run).now();

    return Long.toString(key.getId());
  }
  
  @POST
  @Path("run/{number}/results")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response postResult(@PathParam("number") long runNumber, 
                             @FormParam("name") String benchmarkName, 
                             @FormParam("completed") boolean completed,
                             @FormParam("runTime") long runTime) {


    BenchmarkRun run = PackageDatabase.getBenchmarkRun(runNumber).now();
    if(run == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("Unknown run number " + runNumber).build();
    }

    BenchmarkResult result = new BenchmarkResult();
    result.setRunId(runNumber);
    result.setHarnessVersion(run.getHarnessVersion());
    result.setMachineId(run.getMachineId());
    result.setBenchmarkName(benchmarkName);
    result.setCompleted(completed);
    result.setInterpreter(run.getInterpreter());
    result.setInterpreterVersion(run.getInterpreterVersion());
    result.setRunVariables(run.getRunVariables());
    
    if(completed) {
      result.setRunTime(runTime);
      
      // Update the summary for this benchmark
      queueSummarize(run.getMachineId(), result.getBenchmarkName());
    }

    ObjectifyService.ofy().save().entity(result).now();
    
    return Response.ok().build();
  }

  private long nextRunNumber() {
    return ObjectifyService.ofy().transact(new Work<Long>() {
      @Override
      public Long run() {
        BenchmarkNumber number = ObjectifyService.ofy().load().key(BenchmarkNumber.nextKey()).now();
        
        long nextNumber;
        
        if(number == null) {
          nextNumber = 1;
          number = new BenchmarkNumber();
          number.setNumber(2);
        } else {
          nextNumber = number.getNumber();
          number.setNumber(number.getNumber() + 1);
        }
        ObjectifyService.ofy().save().entity(number);

        return nextNumber;
      }
    });
  }
  
  private void createMachineIfNotExists(final MachineDescriptor descriptor) {
    // Create the machine if doesn't exist
    ObjectifyService.ofy().transact(new Work<Void>() {
      @Override
      public Void run() {
        BenchmarkMachine machine = ObjectifyService.ofy().load().key(
            Key.create(BenchmarkMachine.class, descriptor.getId()))
            .now();

        if(machine == null) {
          machine = new BenchmarkMachine();
          machine.setId(descriptor.getId());
        }
        machine.setOperatingSystem(descriptor.getOperatingSystem());
        machine.setPhysicalMemory(descriptor.getPhysicalMemory());
        machine.setAvailableProcessors(descriptor.getAvailableProcessors());
        machine.setCpuModel(descriptor.getCpuModel());
        
        machine.setLastUpdated(new Date());

        ObjectifyService.ofy().save().entity(machine);

        return null;
      }
    });
  }

}
