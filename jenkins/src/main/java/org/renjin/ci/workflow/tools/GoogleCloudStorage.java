package org.renjin.ci.workflow.tools;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.io.Closer;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.AbortException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jenkinsci.plugins.workflow.actions.LogAction;
import org.renjin.ci.storage.StorageKeys;
import org.renjin.ci.workflow.PackageBuildContext;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GoogleCloudStorage {


    /**
     * Fetches credentials from the Google OAuth Plugin.
     */
    public static Credential fetchCredentials() throws IOException {
        GoogleRobotCredentials credentials = GoogleRobotCredentials.getById("renjinci");
        if(credentials == null) {
            throw new AbortException("No service key credential available for project renjin ci");
        }
        Credential googleCredential;
        try {
            googleCredential = credentials.getGoogleCredential(new GoogleCloudStorageRequirements());
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
        return googleCredential;
    }

    public static Storage newClient() throws IOException {
        return new Storage.Builder(new NetHttpTransport(), new JacksonFactory(),
                fetchCredentials())
                .setApplicationName("Renjin CI")
                .build();
    }


    private static TarArchiveInputStream fetchSource(PackageBuildContext context) throws IOException {
        Storage storage = GoogleCloudStorage.newClient();
        Storage.Objects.Get request = storage.objects().get(
                StorageKeys.PACKAGE_SOURCE_BUCKET,
                StorageKeys.packageSource(context.getPackageVersionId()));

        context.getLogger().println(String.format("Retrieving sources from gs://%s/%s...", request.getBucket(), request.getObject()));

        InputStream inputStream = request.executeMediaAsInputStream();
        return new TarArchiveInputStream(new GZIPInputStream(inputStream));
    }


    private static String stripPackageDir(String name) {
        int slash = name.indexOf('/');
        return name.substring(slash+1);
    }


    public static void downloadAndUnpackSources(PackageBuildContext context) throws IOException, InterruptedException {

        Closer closer = Closer.create();
        TarArchiveInputStream tarIn = closer.register(fetchSource(context));

        try {
            TarArchiveEntry entry;
            while((entry=tarIn.getNextTarEntry())!=null) {
                if(entry.isFile()) {
                    context.workspaceChild(stripPackageDir(entry.getName())).copyFrom(tarIn);
                }
            }
        } catch(Exception e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }


    public static void archiveLogFile(PackageBuildContext build) throws IOException, InterruptedException {

        Storage storage = GoogleCloudStorage.newClient();

        String objectName = StorageKeys.buildLog(build.getPackageVersionId(), build.getBuildNumber());
        StorageObject objectMetadata = new StorageObject()
                .setName(objectName)
                .setContentType("text/plain")
                .setContentEncoding("gzip");

        LogAction logAction = build.getFlowNode().getAction(LogAction.class);
        File tempFile = File.createTempFile("build", ".log");
        try {
            build.getListener().getLogger().printf("Writing log to temp file: " + tempFile);

            OutputStream out = new GZIPOutputStream(new FileOutputStream(tempFile));
            logAction.getLogText().writeLogTo(0, out);
            out.close();

            Storage.Objects.Insert request = storage.objects().insert(
                    StorageKeys.BUILD_LOG_BUCKET,
                    objectMetadata,
                    new FileContent("text/plain", tempFile));

            request.setPredefinedAcl("publicread");
            request.setContentEncoding("gzip");

            request.execute();
        } finally {
            try {
                boolean deleted = tempFile.delete();
                if(!deleted) {
                    build.getListener().getLogger().println("Failed to remove temporary log file");
                }
            } catch (Exception e) {
                build.getListener().getLogger().println("Exception removing temporary log file: " + e.getMessage());
            }
        }

        build.getListener().getLogger().print("Archived build log to ");
        build.getListener().hyperlink(StorageKeys.buildLogUrl(build.getPackageBuild().getId()), StorageKeys.PACKAGE_SOURCE_BUCKET);
        build.getListener().getLogger().println();

    }

}
