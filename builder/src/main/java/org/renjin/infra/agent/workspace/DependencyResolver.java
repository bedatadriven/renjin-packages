package org.renjin.infra.agent.workspace;

import com.google.common.collect.Lists;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.connector.wagon.WagonProvider;
import org.eclipse.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.eclipse.aether.version.Version;
import org.sonatype.maven.wagon.AhcWagon;

import java.io.File;
import java.util.List;

public class DependencyResolver {
  private final List<RemoteRepository> repositories = Lists.newArrayList();
  private RepositorySystem system;
  private DefaultRepositorySystemSession session;

  public DependencyResolver(File localRepoDir) {
     /*
     * Aether's components implement org.eclipse.aether.spi.locator.Service to ease manual wiring and using the
     * prepopulated DefaultServiceLocator, we only need to register the repository connector factories.
     */
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        System.out.println("Maven repo system failed for " + type.getName());
        exception.printStackTrace();
      }
    });
    locator.addService( RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class );
    locator.addService( RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class );
    locator.setServices( WagonProvider.class, new ManualWagonProvider() );

    system = locator.getService( RepositorySystem.class );

    session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository( localRepoDir );

    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );

    this.session = session;

//    repositories.add( new RemoteRepository.Builder( "central", "default", "http://repo1.maven.org/maven2/" ).build() );
    repositories.add( new RemoteRepository.Builder( "renjin", "default", "http://nexus.bedatadriven.com/content/groups/public/" ).build() );
  }


  public String resolveClassPath(String gav) throws DependencyResolutionException, DependencyCollectionException {

    Dependency dependency =
      new Dependency( new DefaultArtifact( gav ), "compile" );

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(dependency);
    DependencyNode node = system.collectDependencies( session, collectRequest ).getRoot();

    DependencyRequest dependencyRequest = new DependencyRequest();
    dependencyRequest.setRoot( node );

    system.resolveDependencies( session, dependencyRequest  );

    PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
    node.accept(nlg);
    return nlg.getClassPath();
  }

  public static class ManualWagonProvider
    implements WagonProvider
  {

    public Wagon lookup( String roleHint )
      throws Exception
    {
      if ( "http".equals( roleHint ) )
      {
        return new AhcWagon();
      }
      return null;
    }

    public void release( Wagon wagon )
    {

    }

  }

}
