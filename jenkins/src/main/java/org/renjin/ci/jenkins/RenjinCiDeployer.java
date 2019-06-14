package org.renjin.ci.jenkins;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.maven.*;
import hudson.maven.reporters.MavenArtifact;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.InvocationInterceptor;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.renjin.ci.RenjinCiClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RenjinCiDeployer extends MavenReporter  {

  /**
   * Accumulates {@link File}s that are created from assembly plugins.
   * Note that some of them might be attached.
   */
  private transient List<String> artifacts;

  @DataBoundConstructor
  public RenjinCiDeployer() {
  }

  @Override
  public boolean preBuild(MavenBuildProxy build, MavenProject pom, BuildListener listener) throws InterruptedException, IOException {
    listener.getLogger().println("[Renjin] " + toString() + " preBuild on " + pom.getArtifactId());
    artifacts = new ArrayList<>();
    return true;
  }

  @Override
  public boolean preExecute(MavenBuildProxy build, MavenProject pom, MojoInfo mojo, BuildListener listener) throws InterruptedException, IOException {

    if(artifacts == null) {
      artifacts = new ArrayList<>();
    }

    if(mojo.is("org.apache.maven.plugins","maven-assembly-plugin","assembly")) {

      try {
        mojo.intercept("assemblyArchiver",new InvocationInterceptor() {
          public Object invoke(Object proxy, Method method, Object[] args, InvocationHandler delegate) throws Throwable {
            Object ret = delegate.invoke(proxy, method, args);
            if(method.getName().equals("createArchive") && method.getReturnType()==File.class) {
//                            System.out.println("Discovered "+ret+" at "+MavenArtifactArchiver.this);
              File f = (File) ret;
              if(!f.isDirectory())
                artifacts.add(f.getAbsolutePath());
            }
            return ret;
          }
        });
      } catch (NoSuchFieldException e) {
        listener.getLogger().println("[JENKINS] Failed to monitor the execution of the assembly plugin: "+e.getMessage());
      }
    }
    return true;
  }

  public boolean postBuild(MavenBuildProxy build, MavenProject pom, final BuildListener listener) throws InterruptedException, IOException {

    listener.getLogger().println("[Renjin] " + toString() + " postBuild on " + pom.getArtifactId());
    try {
      if (pom.getFile() != null) {// goals like 'clean' runs without loading POM, apparently.
        // record POM
        File qualifiedPom = new File(pom.getFile().getParentFile(),
            pom.getArtifactId() + "-" + pom.getVersion() + ".pom");
        Files.copy(pom.getFile(), qualifiedPom);

        artifacts.add(qualifiedPom.getAbsolutePath());

        // record main artifact (if packaging is POM, this doesn't exist)
        final MavenArtifact mainArtifact = MavenArtifact.create(pom.getArtifact());
        if (mainArtifact != null) {
          artifacts.add(pom.getArtifact().getFile().getAbsolutePath());
        }

        // record attached artifacts
        for (Artifact a : pom.getAttachedArtifacts()) {
          MavenArtifact ma = MavenArtifact.create(a);
          if (ma != null) {
            artifacts.add(a.getFile().getAbsolutePath());
          }
        }
      }

      if (!artifacts.isEmpty()) {
        final RenjinArtifactSet artifactSet = new RenjinArtifactSet(new ArrayList<String>(artifacts));
        build.execute(new MavenBuildProxy.BuildCallable<Void, IOException>() {
          private static final long serialVersionUID = -7955474564875700905L;

          public Void call(MavenBuild build) throws IOException, InterruptedException {
            build.replaceAction(artifactSet);
            return null;
          }
        });
      }
    } catch (Exception e) {
      listener.getLogger().println("[Renjin] Exception: " + e.getMessage());
      e.printStackTrace(listener.getLogger());
    }
    return true;
  }

  @Override
  public boolean end(MavenBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

    listener.getLogger().println("[Renjin] " + toString() + " complete, success = " + build.getResult());

    if(build.getResult() == null || build.getResult().isWorseThan(Result.SUCCESS)) {
      return true;
    }


    RenjinArtifactSet artifacts = build.getAction(RenjinArtifactSet.class);

    if(artifacts == null || artifacts.getPaths().isEmpty()) {
      listener.getLogger().println("[Renjin] No artifacts, stopping.");
      return true;
    }

    String objectPath = build.getProject().getGroupId() + "/" +
        build.getProject().getArtifactId() + "/" +
        build.getProject().getVersion() + "/";

    List<String> objectNames = new ArrayList<>();
    for (String path : artifacts.getPaths()) {
      objectNames.add(objectPath + new File(path).getName());
    }

    List<String> cmd = new ArrayList<>();
    cmd.add("gsutil");
    cmd.add("-m");
    cmd.add("cp");

    FilePath workspace = build.getRootBuild().getWorkspace();

    for (String artifact : artifacts.getPaths()) {
      listener.getLogger().println("[Renjin] Deploying " + artifact);

      cmd.add(artifact);
      cmd.addAll(computeDigests(workspace.child(artifact), listener));
    }

    cmd.add("gs://renjinci-artifacts/" + objectPath);

    launcher
        .launch()
        .cmds(cmd)
        .stdout(listener)
        .start()
        .join();

    RenjinCiClient.registerArtifacts("?", objectNames);

    return true;
  }

  public static List<String> computeDigests(FilePath artifactFile, TaskListener listener) {
    try {
      return artifactFile.act(new FilePath.FileCallable<List<String>>() {
        @Override
        public List<String> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {

          Hasher sha1 = Hashing.sha1().newHasher();
          Hasher md5 = Hashing.md5().newHasher();

          byte buffer[] = new byte[8 * 1024];
          try(FileInputStream in = new FileInputStream(f)) {
            int bytesRead;
            while((bytesRead = in.read(buffer, 0, buffer.length)) != -1) {
              sha1.putBytes(buffer, 0, bytesRead);
              md5.putBytes(buffer, 0, bytesRead);
            }
          }

          return Arrays.asList(
              writeHash(f, ".md5", md5.hash()),
              writeHash(f, ".sha1", sha1.hash()));
        }

        private String writeHash(File f, String hashName, HashCode hashCode) throws IOException {
          File hashFile = new File(f.getAbsolutePath() + hashName);
          try(FileWriter writer = new FileWriter(hashFile)) {
            writer.append(hashCode.toString());
          }
          return hashFile.getAbsolutePath();
        }

        @Override
        public void checkRoles(RoleChecker roleChecker) throws SecurityException {
        }
      });
    } catch (Exception e) {
      listener.error("[Renjin] Failed to compute digests");
      e.printStackTrace(listener.getLogger());
    }

    return Collections.emptyList();
  }

  @Extension
  public static final class DescriptorImpl extends MavenReporterDescriptor {
    public String getDisplayName() {
      return "Deploy artifacts to Renjin CI";
    }

  }
}
