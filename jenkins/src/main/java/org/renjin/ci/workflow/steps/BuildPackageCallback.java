package org.renjin.ci.workflow.steps;

import org.codehaus.groovy.ast.PackageNode;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.renjin.ci.model.PackageVersionId;

/**
 * 
 */
public class BuildPackageCallback extends BodyExecutionCallback {
  
  private PackageVersionId packageVersionId;

  public BuildPackageCallback(PackageVersionId packageVersionId) {
    this.packageVersionId = packageVersionId;
  }

  @Override
  public void onSuccess(StepContext context, Object result) {
    
  }

  @Override
  public void onFailure(StepContext context, Throwable t) {

  }
}
