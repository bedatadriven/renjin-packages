package org.renjin.ci.benchmarks;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Work;
import org.renjin.ci.model.BenchmarkEnvironment;
import org.renjin.ci.model.BenchmarkResult;
import org.renjin.ci.model.RenjinRelease;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

@Path("/benchmarks")
public class BenchmarksResource {
    
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(final SubmissionSet resultSet) {
        createEnvironmentIfNotExists(resultSet);
        
        String interpreterCommitId = lookupRenjinCommit(resultSet.getInterpreterVersion());

        List<BenchmarkResult> toSave = Lists.newArrayList();
        for (Submission submission : resultSet.getResults()) {
            
            BenchmarkResult result = new BenchmarkResult();
            result.setEnvironmentId(resultSet.getEnvironmentId());
            result.setInterpreter(resultSet.getInterpreter());
            result.setInterpreterVersion(resultSet.getInterpreterVersion());
            result.setInterpreterCommitId(interpreterCommitId);
            result.setBenchmarkId(submission.getBenchmarkId());
            result.setTime(new Date(submission.getTime()));
            result.setValue(submission.getValue());
            result.setId(result.computeHash());
            toSave.add(result);
        }
        
        ObjectifyService.ofy().save().entities(toSave);
        
        return Response.ok().build();
    }
    
    @GET
    @Path("/results")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BenchmarkResult> queryResults() {
        QueryResultIterable<BenchmarkResult> results = ObjectifyService.ofy().load()
                .type(BenchmarkResult.class)
                .iterable();
        
        return Lists.newArrayList(results);
    }
    
    @GET
    @Path("/environments")
    public List<BenchmarkEnvironment> getEnvironments() {
        QueryResultIterable<BenchmarkEnvironment> results = ObjectifyService.ofy().load()
                .type(BenchmarkEnvironment.class)
                .iterable();

        return Lists.newArrayList(results);
    }

    private String lookupRenjinCommit(String interpreterVersion) {
        RenjinRelease release = ObjectifyService.ofy().load().key(Key.create(RenjinRelease.class, interpreterVersion)).now();
        if(release == null) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Invalid renjin version '" + interpreterVersion + "'")
                    .build());
        }
        return release.getRenjinCommit().getKey().getName();
    }


    private void createEnvironmentIfNotExists(final SubmissionSet resultSet) {
        // Create the environment if doesn't exist
        ObjectifyService.ofy().transact(new Work<Void>() {
            @Override
            public Void run() {
                BenchmarkEnvironment environment = ObjectifyService.ofy().load().key(
                        Key.create(BenchmarkEnvironment.class, resultSet.getEnvironmentId()))
                        .now();
                
                if(environment == null) {
                    environment = new BenchmarkEnvironment();
                    environment.setId(resultSet.getEnvironmentId());
                    ObjectifyService.ofy().save().entity(environment);
                }
                
                return null;
            }
        });
    }

}
