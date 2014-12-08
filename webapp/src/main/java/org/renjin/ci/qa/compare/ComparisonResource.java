package org.renjin.ci.qa.compare;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Work;
import org.glassfish.jersey.server.mvc.Viewable;
import org.renjin.ci.model.PackageDatabase;
import org.renjin.ci.model.RenjinVersionId;
import org.renjin.ci.model.VersionComparison;
import org.renjin.ci.model.VersionComparisonReport;
import org.renjin.ci.pipelines.Pipelines;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static com.google.appengine.tools.pipeline.PipelineServiceFactory.newPipelineService;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Compares Renjin Releases
 */
public class ComparisonResource {

  private RenjinVersionId fromVersion;
  private RenjinVersionId toVersion;

  public ComparisonResource(RenjinVersionId from, RenjinVersionId to) {
    this.fromVersion = from;
    this.toVersion = to;
  }

  @GET
  @Path("report")
  @Produces("text/html")
  public Viewable compareReleases() {

    VersionComparison comparison = PackageDatabase.getVersionComparison(fromVersion, toVersion);
    ComparisonViewModel viewModel = new ComparisonViewModel(comparison);

    return new Viewable("/compare.ftl", viewModel);
  }

  @POST
  @Path("generate")
  public Response startGeneration() {

    // Create a new report for this comparison
    final long reportId = VersionComparisonReport.newReportId();
    String jobId = newPipelineService().startNewPipeline(new GenerateReport(reportId, fromVersion, toVersion));

    final VersionComparisonReport report = new VersionComparisonReport(reportId);
    report.setJobId(jobId);
    report.setComplete(false);
    ofy().save().entity(report);

    ofy().transactNew(new Work<VersionComparison>() {
      @Override
      public VersionComparison run() {
        // Set this report to the latest report
        VersionComparison comparison = PackageDatabase.getVersionComparison(fromVersion, toVersion);
        comparison.setReport(Ref.create(Key.create(VersionComparisonReport.class, reportId)));

        ofy().save().entities(comparison);

        return comparison;
      }
    });

    return Pipelines.redirectToStatus(jobId);
  }
}
