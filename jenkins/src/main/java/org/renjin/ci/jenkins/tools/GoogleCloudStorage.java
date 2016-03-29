package org.renjin.ci.jenkins.tools;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.FilePath;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.renjin.ci.build.PackageBuild;
import org.renjin.ci.jenkins.BuildContext;
import org.renjin.ci.jenkins.ConfigException;
import org.renjin.ci.jenkins.WorkerContext;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;

public class GoogleCloudStorage {

    public static final String PROJECT_ID = "renjinci";

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudStorage.class.getName());

    /**
     * Fetches credentials from the Google OAuth Plugin.
     * @param context
     */
    public static Credential fetchCredentials(WorkerContext context) throws IOException {
        GoogleRobotCredentials credentials = GoogleRobotCredentials.getById(PROJECT_ID);
        if(credentials == null) {
            throw new ConfigException(format("No service key credential available for project %s", PROJECT_ID));
        }
        
        Credential googleCredential;
        try {
            googleCredential = credentials.getGoogleCredential(new GoogleCloudStorageRequirements());
        } catch (GeneralSecurityException e) {
            context.getLogger().println("ERROR: Exception obtaining credentials for package source repo: " + e.getMessage());
            throw new IOException(e);
        }
        return googleCredential;
    }

    public static Storage newClient(WorkerContext context) throws IOException {
        return new Storage.Builder(new NetHttpTransport(), new JacksonFactory(),
                fetchCredentials(context))
                .setApplicationName("Renjin CI")
                .build();
    }


    private static TarArchiveInputStream fetchSource(BuildContext context, PackageVersionId packageVersionId) throws IOException {
        Storage storage = GoogleCloudStorage.newClient(context.getWorkerContext());
        Storage.Objects.Get request = storage.objects().get(
                StorageKeys.PACKAGE_SOURCE_BUCKET,
                StorageKeys.packageSource(packageVersionId));

        context.log("Retrieving sources from gs://%s/%s...", request.getBucket(), request.getObject());

        try {
            InputStream inputStream = request.executeMediaAsInputStream();
            return new TarArchiveInputStream(new GZIPInputStream(inputStream));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error downloading sources", e);

            context.getLogger().println(format("ERROR: IOException downloading sources: %s", e.getMessage()));
            throw e;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error downloading sources", e);

            context.log("ERROR: %s downloading sources: %s", e.getClass().getName(), e.getMessage());

            throw new IOException(e);
        }
    }


    private static String stripPackageDir(String name) {
        int slash = name.indexOf('/');
        return name.substring(slash+1);
    }


    public static void downloadAndUnpackSources(BuildContext context, PackageVersionId packageVersionId) throws IOException, InterruptedException {

        Closer closer = Closer.create();
        TarArchiveInputStream tarIn = closer.register(fetchSource(context, packageVersionId));
        try {

            TarArchiveEntry entry;
            while((entry=tarIn.getNextTarEntry())!=null) {
                if(entry.isFile()) {
                    context.getBuildDir().child(stripPackageDir(entry.getName())).copyFrom(tarIn);
                }
            }
        } catch(Exception e) {
            context.log("Failed to fetch package sources: " + e.getMessage());
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
        
        // Band-aid required for compiling older files:
        FilePath namespaceFile = context.getBuildDir().child("NAMESPACE");
        if(!namespaceFile.exists()) {
            namespaceFile.write("exportPattern( \".\" )\n", Charsets.UTF_8.name());
        }
    }

    public static LogArchiver newArchiver(BuildContext context, PackageBuild build) throws IOException {
        return new LogArchiver(context, build, newClient(context.getWorkerContext()));
    }

    public static void archiveLogFile(BuildContext buildContext, PackageBuild build) throws IOException, InterruptedException {

        Storage storage = GoogleCloudStorage.newClient(buildContext.getWorkerContext());

        String objectName = StorageKeys.buildLog(build.getPackageVersionId(), build.getBuildNumber());
        StorageObject objectMetadata = new StorageObject()
                .setName(objectName)
                .setContentType("text/plain")
                .setContentEncoding("gzip");

        Storage.Objects.Insert request = storage.objects().insert(
                StorageKeys.BUILD_LOG_BUCKET,
                objectMetadata,
                new FileContent("text/plain", buildContext.getLogFile()));

        request.setPredefinedAcl("publicread");
        request.setContentEncoding("gzip");

        request.execute();
   
        buildContext.log("Successfully archived log file");
    }



}
