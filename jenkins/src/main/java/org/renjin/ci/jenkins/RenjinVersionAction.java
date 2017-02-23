package org.renjin.ci.jenkins;

import hudson.model.Action;
import hudson.model.InvisibleAction;
import org.renjin.ci.model.RenjinVersionId;

/**
 * Marks a build as having been built against a specific version of Renjin
 */
public class RenjinVersionAction implements Action {

    private String version;

    public RenjinVersionAction() {
    }

    public RenjinVersionAction(RenjinVersionId versionId) {
        this.version = versionId.toString();
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Renjin " + version;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
