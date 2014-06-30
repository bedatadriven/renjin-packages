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

package org.renjin.build.worker;

import com.google.common.io.ByteSource;
import org.codehaus.jackson.node.ObjectNode;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.renjin.build.storage.StorageKeys;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Client to retrieve artifacts from Google Cloud Storage and
 * Post build artifacts
 */
public class StorageClient {

  public static final StorageClient INSTANCE = new StorageClient();

  private final Client client;
  private final String accessToken;


  public StorageClient() {
    client = ClientBuilder.newClient().register(JacksonFeature.class);
    accessToken = fetchAccessToken();
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
    String uri = "https://storage.googleapis.com/" +
        StorageKeys.PACKAGE_SOURCE_BUCKET + "/" +
        StorageKeys.packageSource(gav[0], gav[1], gav[2]);

    return client
        .target(uri)
        .request()
        .header("Authorization", "OAuth " + accessToken)
        .get(InputStream.class);
  }

  /**
   * Posts a build log to Google Storage
   */
  public void putBuildLog(long buildNumber, String packageVersionId, ByteSource input) throws IOException {

    String uri = "https://storage.googleapis.com/" +
        StorageKeys.BUILD_LOG_BUCKET + "/" + StorageKeys.buildLog(buildNumber, packageVersionId);

    client
        .target(uri)
        .request()
        .header("Authorization", "OAuth " + accessToken)
        .header("x-goog-acl", "public-read")
        .put(gzip(input));
  }

  private Entity gzip(ByteSource input) throws IOException {

    // GZIP the log text
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    GZIPOutputStream gz = new GZIPOutputStream(baos);
    input.copyTo(gz);
    gz.close();

    // Create a variant describing mediatype
    Variant variant = new Variant(MediaType.TEXT_PLAIN_TYPE, (String) null, "gzip");
    return Entity.entity(baos.toByteArray(), variant);
  }
}