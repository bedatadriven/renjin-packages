package org.renjin.ci;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class NoRobotsFeature implements DynamicFeature {


  @Override
  public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
    if(resourceInfo.getResourceMethod().isAnnotationPresent(NoRobots.class) ||
        resourceInfo.getResourceClass().isAnnotationPresent(NoRobots.class)) {

      featureContext.register(NoRobotsFilter.class);
    }
  }
}
