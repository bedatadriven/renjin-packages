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

package org.renjin.cran;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.jdbc.AbstractWork;
import org.renjin.repo.model.RPackageBuildResult;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class BuildLogUploader {

  /** E-mail address of the service account. */
  private static final String SERVICE_ACCOUNT_EMAIL = "213809300358@developer.gserviceaccount.com";

  /** Bucket to list. */
  private static final String BUCKET_NAME = "renjin-build-logs";

  /** Global configuration of Google Cloud Storage OAuth 2.0 scope. */
  private static final String STORAGE_SCOPE =
    "https://www.googleapis.com/auth/devstorage.read_write";


  /** Global instance of the HTTP transport. */
  private HttpTransport httpTransport;

  /** Global instance of the JSON factory. */
  private final JsonFactory JSON_FACTORY = new JacksonFactory();
  private final GoogleCredential credential;

  private int buildId;

  public BuildLogUploader(int buildId) {
    this.buildId = buildId;

    try {
      httpTransport =  new NetHttpTransport();


      KeyStore keystore = KeyStore.getInstance("PKCS12");
      keystore.load(BuildLogUploader.class.getResourceAsStream("/key.p12"), "notasecret".toCharArray());
      PrivateKey key = (PrivateKey)keystore.getKey("privatekey", "notasecret".toCharArray());

      // Build service account credential.
      credential = new GoogleCredential.Builder().setTransport(httpTransport)
        .setJsonFactory(JSON_FACTORY)
        .setServiceAccountId(SERVICE_ACCOUNT_EMAIL)
        .setServiceAccountScopes(Collections.singleton(STORAGE_SCOPE))
        .setServiceAccountPrivateKey(key)
        .build();

    } catch(Exception e) {
      throw new RuntimeException("Could not initialize BuildLogUploader", e);
    }

  }

  public void put(String packageVersionId, InputSupplier<? extends InputStream> input) throws IOException {
    String URI = "https://storage.googleapis.com/" + BUCKET_NAME + "/" + uriFor( packageVersionId);
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
    GenericUrl url = new GenericUrl(URI);
    HttpRequest request = requestFactory.buildPutRequest(url, new LogFileContent(input));

    HttpHeaders headers = new HttpHeaders();
    headers.set("x-goog-acl", "public-read");
    request.setHeaders(headers);

    HttpResponse response = request.execute();

    System.out.println(response.getStatusCode());
  }

  private String uriFor(String packageVersionId) {
    return buildId + "/" + packageVersionId.replace(':', '/') + ".log";
  }


  public static class LogFileContent extends AbstractHttpContent {

    private final InputSupplier<? extends InputStream> input;

    public LogFileContent(InputSupplier<? extends InputStream> input) {
      this.input = input;
    }

    @Override
    public String getType() {
      return "text/plain";
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
      GZIPOutputStream gz = new GZIPOutputStream(outputStream);
      ByteStreams.copy(input, gz);
      gz.finish();
    }

    @Override
    public String getEncoding() {
      return "gzip";
    }
  }

  public static void main(String[] args) throws IOException {

    PersistenceUtil.createEntityManager().unwrap(HibernateEntityManager.class).getSession().doWork(new AbstractWork() {
      @Override
      public void execute(Connection connection) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate("alter table RPackageBuildResult drop column log");
      }
    });

  }
}