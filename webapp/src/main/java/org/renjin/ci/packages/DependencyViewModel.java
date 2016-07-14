package org.renjin.ci.packages;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import org.renjin.ci.model.PackageDependency;
import org.renjin.ci.model.PackageVersionId;

public class DependencyViewModel {

    private PackageDependency declared;
    private PackageVersionId resolved;

    public DependencyViewModel(PackageDependency declared, PackageVersionId resolved) {
        this.declared = declared;
        this.resolved = resolved;
    }

    public PackageDependency getDeclared() {
        return declared;
    }

    public PackageVersionId getResolved() {
        return resolved;
    }
    
    public String getLabel() {
        StringBuilder label = new StringBuilder();
        label.append(declared.getName());
        if(resolved != null) {
            label.append(" ");
            label.append(resolved.getVersionString());
        } else if(!Strings.isNullOrEmpty(declared.getVersionSpec())) {
            label.append(" ");
            label.append(declared.getVersionSpec());
        }
        return label.toString();
    }
    
    public String getName() {
        return declared.getName();
    }
    
    public String getUrl() {
        if(resolved == null) {
            return null;
        } else {
            return "/packages/" + resolved.getGroupId() + "/" + resolved.getPackageName() + "/" + resolved.getVersion();
        }
    }
}
