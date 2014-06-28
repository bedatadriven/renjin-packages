package org.renjin.build.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.appengine.api.urlfetch.*;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import javax.mail.Multipart;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.appengine.api.urlfetch.FetchOptions.Builder.doNotValidateCertificate;

public class JenkinsClient {

  private static final Logger LOGGER = Logger.getLogger(JenkinsClient.class.getName());

  public static final String USER_NAME = "renjin";
  public static final String PASSWORD = "pAMGz6qEJUdo9";
  public static final String TOKEN = "b6jaOLYKYJe8W";

  private final URLFetchService fetchService;
  private final ObjectMapper objectMapper;

  public JenkinsClient() {
    fetchService = URLFetchServiceFactory.getURLFetchService();
    objectMapper = new ObjectMapper();
  }

  private HTTPHeader authenticationHeader() {
    return new HTTPHeader("Authorization", "Basic " +
        BaseEncoding.base64().encode((USER_NAME + ":" + PASSWORD).getBytes()));
  }

  public String getJobs() throws IOException {
    HTTPRequest request = new HTTPRequest(
        new URL("https://jenkins.bedatadriven.com/queue/api/json?pretty=true"),
        HTTPMethod.GET,
        doNotValidateCertificate());

    request.addHeader(authenticationHeader());

    HTTPResponse response = fetchService.fetch(request);

    return new String(response.getContent());
  }

  public void start(Job job) throws Exception {

    Multipart multipart = job.buildMultiPart();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    multipart.writeTo(baos);


    System.out.println("Content-Type: " + multipart.getContentType());
    System.out.println(new String(baos.toByteArray()));


    URL url = new URL("https://jenkins.bedatadriven.com/job/Renjin-Package/build");
    HTTPRequest request = new HTTPRequest(url, HTTPMethod.POST, doNotValidateCertificate());
    request.addHeader(authenticationHeader());
    request.setHeader(new HTTPHeader("Content-Type", multipart.getContentType()));
    request.setPayload(baos.toByteArray());

    HTTPResponse response = fetchService.fetch(request);

    LOGGER.log(Level.INFO, "Build status: " + response.getResponseCode());

    LOGGER.log(Level.INFO, "Build response: " + new String(response.getContent()));
  }
}
