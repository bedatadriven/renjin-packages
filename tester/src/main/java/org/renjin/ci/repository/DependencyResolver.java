package org.renjin.ci.repository;

import com.google.common.collect.Lists;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.connector.wagon.WagonProvider;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.renjin.ci.model.PackageBuildId;
import org.renjin.ci.model.RenjinVersionId;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

public class DependencyResolver {
  private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

  private final List<RemoteRepository> repositories = Lists.newArrayList();
  private final RepositorySystem system = newRepositorySystem();
  private final RepositorySystemSession session;

  public DependencyResolver() throws IOException {
    repositories.add(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());
    repositories.add(new RemoteRepository.Builder("bdd", "default", "http://nexus.bedatadriven.com/content/groups/public").build());
    repositories.add(new RemoteRepository.Builder("renjin-ci", "default", "http://ci.repo.renjin.org/").build());
    session = newRepositorySystemSession(system);
  }

  public List<URL> resolveRenjin(RenjinVersionId renjinVersion) throws Exception {
    DefaultArtifact renjinArtifact = new DefaultArtifact(
        "org.renjin", "renjin-cli", "jar", renjinVersion.toString());
    return resolveArtifact(renjinArtifact, null);
  }

  /**
   * Resolves all dependencies of a given package
   * @return list of resolved URLs pointing to the local repo
   * @throws Exception
   * @param artifact
   */
  public List<URL> resolvePackage(PackageBuildId buildId) throws Exception {
    
    DefaultArtifact packageArtifact = new DefaultArtifact(
        buildId.getGroupId(), 
        buildId.getPackageName(), "jar", 
        buildId.getBuildVersion());
    
    return resolveArtifact(packageArtifact, new RenjinExclusionFilter());
  }

  private List<URL> resolveArtifact(DefaultArtifact renjinArtifact, DependencyFilter filter) throws Exception {
    List<URL> artifacts = Lists.newArrayList();

    Dependency renjinDependency = new Dependency(renjinArtifact, "runtime");
    CollectRequest collectRequest = new CollectRequest(renjinDependency, repositories);
    
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
    dependencyRequest.setFilter(filter);
    
    DependencyResult dependencyResult = system.resolveDependencies(session, dependencyRequest);

    for(ArtifactResult artifactResult : dependencyResult.getArtifactResults()) {
      artifacts.add(artifactResult.getArtifact().getFile().toURI().toURL());
    }

    return artifacts;
  }

  public static RepositorySystem newRepositorySystem() {
    /*
    * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
    * prepopulated DefaultServiceLocator, we only need to register the repository connector factories.
    */
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
    locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
    locator.setServices(WagonProvider.class, new ManualWagonProvider());

    return locator.getService(RepositorySystem.class);
  }


  public static DefaultRepositorySystemSession newRepositorySystemSession(RepositorySystem system) throws IOException {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    File tempDir = new File(System.getProperty("java.io.tmpdir"));
    if(!tempDir.exists()) {
      throw new AssertionError(tempDir + " does not exist");
    }

    File localTestRepo = new File(tempDir, "renjin-test-repo");
    if(!localTestRepo.exists()) {
      Files.createDirectory(localTestRepo.toPath());
    }

    LOGGER.info("Using local repository: " + localTestRepo);

    LocalRepository localRepo = new LocalRepository(localTestRepo);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));
    session.setTransferListener(new ConsoleTransferListener());
    session.setRepositoryListener(new ConsoleRepositoryListener());

    return session;
  }
}
