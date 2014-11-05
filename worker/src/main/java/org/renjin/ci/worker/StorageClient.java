/*
 * Copyright (c) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.renjin.ci.worker;

import com.google.appengine.tools.cloudstorage.*;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import org.codehaus.jackson.node.ObjectNode;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.renjin.ci.storage.StorageKeys;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.util.zip.GZIPOutputStream;

/**
 * Client to retrieve artifacts from Google Cloud Storage and
 * Post build artifacts
 */
public class StorageClient {

    private final GcsService gcsService =
            GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

  public static final StorageClient INSTANCE = new StorageClient();

  private final Client client;


  public StorageClient() {
    client = ClientBuilder.newClient().register(JacksonFeature.class);
  }

  /**
   * Fetches an access token from the GCE metadata server
   * @see <a href="https://developers.google.com/compute/docs/authentication#using">GCE Documentation</a>
   * @return the access token
   */
  public String fetchAccessToken() {
    return client
        .target("http://metadata/computeMetadata/v1/instance/service-accounts/default/token")
        .request(MediaType.APPLICATION_JSON_TYPE)
        .header("X-Google-Metadata-Request", "True")
        .get(ObjectNode.class)
        .get("access_token").asText();
  }

  public InputStream openSourceArchive(String packageVersionId) throws IOException {
    String gav[] = packageVersionId.split(":");

    GcsFilename filename = new GcsFilename(
        StorageKeys.PACKAGE_SOURCE_BUCKET,
        StorageKeys.packageSource(gav[0], gav[1], gav[2]));

    GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel(filename, 0, 1024 * 1024);
    return Channels.newInputStream(readChannel);
  }

  /**
   * Posts a build log to Google Storage
   */
  public void putBuildLog(long buildNumber, String packageVersionId, ByteSource input) throws IOException {

    GcsFilename filename = new GcsFilename(
        StorageKeys.BUILD_LOG_BUCKET,
        StorageKeys.buildLog(buildNumber, packageVersionId));

    GcsFileOptions options = new GcsFileOptions.Builder()
        .contentEncoding("gzip")
        .mimeType("text/plain")
        .acl("public-read")
        .build();

    GcsOutputChannel outputChannel =
        gcsService.createOrReplace(filename, options);

    try (OutputStream out = new GZIPOutputStream(Channels.newOutputStream(outputChannel))) {
      input.copyTo(out);
    }

  }
}