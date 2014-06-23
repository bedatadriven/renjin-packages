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

package org.renjin.build.agent.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import org.renjin.build.storage.StorageKeys;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Collections;

public class GoogleCloudStorage {

  private static final String APPLICATION_NAME = "renjin-build-agent";

  public static final GoogleCloudStorage INSTANCE = new GoogleCloudStorage();

  /** E-mail address of the service account. */
  private static final String SERVICE_ACCOUNT_EMAIL =
      "135288259907-c92vdks5f43qngv90bm9kirba7k7lntd@developer.gserviceaccount.com";

  /** Global configuration of Google Cloud Storage OAuth 2.0 scope. */
  private static final String STORAGE_SCOPE =
    "https://www.googleapis.com/auth/devstorage.read_write";

  /** Global instance of the HTTP transport. */
  private HttpTransport httpTransport;

  private final GoogleCredential credential;

  public GoogleCloudStorage() {

    try {
      httpTransport =  new NetHttpTransport();

      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(GoogleCloudStorage.class.getResourceAsStream("/key.p12"), "notasecret".toCharArray());
      PrivateKey key = (PrivateKey)keystore.getKey("privatekey", "notasecret".toCharArray());

      JacksonFactory jsonFactory = new JacksonFactory();

      credential = new GoogleCredential.Builder()
        .setTransport(httpTransport)
        .setJsonFactory(jsonFactory)
        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
        .setServiceAccountScopes(Collections.singleton(STORAGE_SCOPE))
        .setServiceAccountPrivateKey(key)
        .build();

    } catch(Exception e) {
      throw new RuntimeException("Could not initialize GoogleCloudStorage", e);
    }
  }

  public InputStream openSourceArchive(String groupId, String packageName, String version) throws IOException {
    String uri = "https://storage.googleapis.com/" +
        StorageKeys.BUCKET_NAME + "/" +
        StorageKeys.packageSource(groupId, packageName, version);

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
    GenericUrl url = new GenericUrl(uri);
    HttpRequest request = requestFactory.buildGetRequest(url);
    HttpResponse response = request.execute();

    if(!response.isSuccessStatusCode()) {
      throw new IOException("Could not open source archive at " + uri);
    }

    return response.getContent();
  }

  /**
   * Posts a build log to Google Storage
   * @param buildId
   * @param packageVersionId
   * @param input
   * @throws IOException
   */
  public void putBuildLog(int buildId, String packageVersionId, ByteSource input) throws IOException {

    String uri = "https://storage.googleapis.com/renjin-build/log/" +
        StorageKeys.BUCKET_NAME + "/" + StorageKeys.buildLog(buildId, packageVersionId);

    HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
    HttpRequest request = requestFactory
        .buildPutRequest(new GenericUrl(uri), new LogFileContent(input))
        .setEncoding(new GZipEncoding());

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-goog-acl", "public-read");
    request.setHeaders(headers);

    HttpResponse response = request.execute();

    if(response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
      System.out.println("Failed to upload build log for " + packageVersionId + ": " +
        response.getStatusCode());
    }
  }

  public static class LogFileContent extends AbstractHttpContent {

    private final ByteSource input;

    public LogFileContent(ByteSource input) {
      super("plain/text");
      this.input = input;
    }

    @Override
    public String getType() {
      return "text/plain";
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      input.copyTo(outputStream);
    }
  }
}