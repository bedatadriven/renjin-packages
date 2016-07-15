package org.renjin.ci.packages;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import org.renjin.ci.datastore.PackageVersion;
import org.renjin.ci.model.PackageId;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.model.ResolvedDependency;
import org.renjin.ci.model.ResolvedDependencySet;

import java.util.*;


public class DependencyResolutionMulti {

  private final List<PackageVersionId> toResolve;
  private final List<PackageVersionId> roots;
  private Multimap<PackageVersionId, PackageVersionId> dependencyMap = HashMultimap.create();
  private Map<PackageId, PackageVersionId> resolvedVersion = new HashMap<>();
  private Set<PackageVersionId> resolved = new HashSet<>();

  private LinkedList<PackageVersionId> sorted = new LinkedList<>();

  public DependencyResolutionMulti(List<PackageVersionId> toResolve) {
    this.roots = new ArrayList<>(toResolve);
    this.toResolve = new ArrayList<>(toResolve);
  }

  public List<PackageVersionId> resolve() {

    // First build a dependency tree of ALL referenced 
    // package/versions
    while(!toResolve.isEmpty()) {
      List<PackageVersion> versions = loadVersions(toResolve);
      for (PackageVersion version : versions) {
        resolve(version);
      }
    }

    // Now do a breadth-first search to resolve transitive
    // dependency conflicts
    resolveConflicts(roots);
    
    
    for (PackageVersionId root : roots) {
      add(root);
    }
    
    return sorted;
  }

  private void resolveConflicts(List<PackageVersionId> parents) {

    List<PackageVersionId> children = Lists.newArrayList();

    for (PackageVersionId versionId : parents) {
      PackageVersionId resolvedVersion = this.resolvedVersion.get(versionId.getPackageId());
      if(resolvedVersion == null || versionId.isNewer(resolvedVersion)) {
        this.resolvedVersion.put(versionId.getPackageId(), versionId);
        children.addAll(dependencyMap.get(versionId));
      } 
    }
    if(!children.isEmpty()) {
      resolveConflicts(children);
    }
  }

  private void add(PackageVersionId versionId) {

    PackageVersionId resolvedId = resolvedVersion.get(versionId.getPackageId());
    if(!sorted.contains(resolvedId)) {
          
      // Add dependencies first
      Collection<PackageVersionId> dependencies = dependencyMap.get(resolvedId);
      for (PackageVersionId dependency : dependencies) {
        add(dependency);
      }
      
      // Now add ourselves
      sorted.add(resolvedId);
    }
  }

  private void resolve(PackageVersion version) {
    DependencyResolution resolution = new DependencyResolution(version);
    ResolvedDependencySet set = resolution.resolve();
    for (ResolvedDependency resolvedDependency : set.getDependencies()) {
      PackageVersionId resolveId = resolvedDependency.getPackageVersionId();
      dependencyMap.put(version.getPackageVersionId(), resolveId);
      if(!resolved.contains(resolveId)) {
        toResolve.add(resolveId);
      }
    }
    resolved.add(version.getPackageVersionId());
  }

  private List<PackageVersion> loadVersions(List<PackageVersionId> toResolve) {
    List<Key<PackageVersion>> keys = new ArrayList<>();
    for (PackageVersionId packageVersionId : toResolve) {
      keys.add(PackageVersion.key(packageVersionId));
    }
    Map<Key<PackageVersion>, PackageVersion> map = ObjectifyService.ofy().load().keys(keys);

    toResolve.clear();
    return new ArrayList<>(map.values());
  }

}
