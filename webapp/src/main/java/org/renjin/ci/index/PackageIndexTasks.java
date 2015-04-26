package org.renjin.ci.index;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.model.*;
import org.renjin.ci.model.Package;
import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;

@Path("/tasks/packages")
public class PackageIndexTasks {

    
    @GET
    public Response get() {
        String jobId = Pipelines.applyAll(new PackageIndexer());
        return Pipelines.redirectToStatus(jobId);
    }
    
    @GET
    @Produces("text/plain")
    @Path("/{packageName}")
    public String indexPackage(@PathParam("packageName") String packageName) {
        Key<Package> packageKey = Key.create(Package.class, "org.renjin.cran:" + packageName);
        Package entity = ObjectifyService.ofy().load().key(packageKey).now();
        new PackageIndexer().apply(entity);

        entity = ObjectifyService.ofy().load().key(packageKey).now();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println("lastGoodBuild: " + entity.getLastGoodBuild());
        pw.println("latestVersion " + entity.getLatestVersion());
        pw.println("renjinRegression " + entity.isRenjinRegression());
        pw.flush();
        
        return sw.toString();
    }
    
    
}
