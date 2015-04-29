package org.renjin.ci.workflow;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistIn;
import org.jenkinsci.plugins.workflow.cps.persistence.PersistenceContext;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.renjin.ci.model.BuildOutcome;
import org.renjin.ci.model.NativeOutcome;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.PackageBuildResult;
import org.renjin.ci.workflow.graph.PackageNode;
import org.renjin.ci.workflow.tools.*;

import javax.inject.Inject;

/**
 * Builds a Renjin Package
 */
public final class BuildPackageExecution extends AbstractSynchronousStepExecution<Boolean> {


  private static final long serialVersionUID = 1L;

  @Inject
  private transient BuildPackageStep step;

  @StepContextParameter
  private transient FlowNode flowNode;
  
  @StepContextParameter
  private transient TaskListener listener;

  /**
   * The build number of this package version
   */
  private Long buildNumber;

  @Override
  protected Boolean run() throws Exception {
    flowNode.replaceAction(new PackageLabelAction(step.getPackageVersionId().toString()));
    flowNode.save();
    
    
    /**
     * First, get the next build sequence number of this package from renjinci.appspot.com
     * and save it with the node so that if we are restarted, we retain the same build number
     */
    if(buildNumber == null) {
      buildNumber = WebApp.startBuild(step);
      flowNode.save();
    }

    listener.getLogger().printf("Starting build #%d of %s...\n",
        buildNumber, step.getLeasedBuild().getPackageVersionId());

    for (PackageNode packageNode : step.getLeasedBuild().getNode().getDependencies()) {
      listener.getLogger().printf("Dependency: %s\n", packageNode.getId());
    }
    listener.getLogger().flush();
    
    

    PackageBuildResult result;

    try {

      PackageBuildContext build = new PackageBuildContext(getContext(), step, buildNumber);

      /**
       * Download and unpack the original source of this package
       */
      GoogleCloudStorage.downloadAndUnpackSources(build);

      /**
       * Generate a POM file for this project that matches the GNU-R style layout
       */
      Maven.writePom(build);

      /**
       * Execute maven, building the jar file and deploying it to repo.renjin.org
       */
      Maven.build(build);

      /**
       * Parse the result of the build from the log files
       */
      result = LogFileParser.parse(build);

      /**
       * Archive the build log file permanently to Google Cloud Storage
       */
      GoogleCloudStorage.archiveLogFile(build);

      /**
       * Report the build result to ci.renjin.org
       */
      WebApp.reportBuildResult(build, result);
      
    } catch (Exception e) {
      result = new PackageBuildResult(BuildOutcome.ERROR);
    }


    /**
     * Update the package graph
     */
    step.getLeasedBuild().completed(listener, buildNumber, result.getOutcome());
    
    /**
     * Update this node's label
     */
    flowNode.replaceAction(new PackageLabelAction(step.getPackageVersionId(), buildNumber, result));
    flowNode.save();
    
    return result.getOutcome() == BuildOutcome.SUCCESS;
  }

  @PersistIn(PersistenceContext.FLOW_NODE)
  public static class PackageLabelAction extends LabelAction {

    private PackageVersionId packageVersionId;
    private Long buildNumber;
    private BuildOutcome buildOutcome;
    private NativeOutcome nativeOutcome;

    public PackageLabelAction(String packageVersionId) {
      super(null);
      this.packageVersionId = PackageVersionId.fromTriplet(packageVersionId);
    }


    public PackageLabelAction(PackageVersionId versionId, long buildNumber, PackageBuildResult result) {
      super(null);
      this.packageVersionId = versionId;
      this.buildNumber = buildNumber;
      this.buildOutcome = result.getOutcome();
      this.nativeOutcome = result.getNativeOutcome();
    }

    @Override
    public String getDisplayName() {
      StringBuilder sb = new StringBuilder();
      sb.append(packageVersionId.getGroupId())
              .append(".")
              .append(packageVersionId.getPackageName())
              .append(" ")
              .append(packageVersionId.getVersion());

      if(buildNumber != null) {
        sb.append(" build ").append(buildNumber);
      }
      if(buildOutcome != null) {
        sb.append(": ").append(buildOutcome.name());
      }

      return sb.toString();
    }

    @Override
    public String getUrlName() {
      StringBuilder url = new StringBuilder();
      url.append("https://renjinci.appspot.com/")
              .append(packageVersionId.getGroupId())
              .append("/")
              .append(packageVersionId.getPackageName())
              .append("/")
              .append(packageVersionId.getVersion());

      if(buildNumber != null) {
        url.append("/build/").append(buildNumber);
      }
      return url.toString();
    }
  }
}
