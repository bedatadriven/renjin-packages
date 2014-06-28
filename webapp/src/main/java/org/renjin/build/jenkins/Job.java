package org.renjin.build.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Job {

  private List<Map<String, String>> parameters = Lists.newArrayList();
  private List<MimeBodyPart> files = Lists.newArrayList();

  public Job() {


  }

  public void addParameter(String name, String value) {
    Map<String, String> param = new HashMap<>();
    param.put("name", name);
    param.put("value", value);
    parameters.add(param);
  }

  public void addFileParameter(String name, String content, String contentType) {

    int fileIndex = files.size();
    String fieldName = "file" + fileIndex;

    Map<String, String> param = new HashMap<>();
    param.put("name", name);
    param.put("file", "file" + files.size());
    parameters.add(param);

    try {
      InternetHeaders headers = new InternetHeaders();
      headers.addHeader("Content-Disposition",
          String.format("form-data; name=\"%s\"; filename=\"%s\"", fieldName, name));
      headers.addHeader("Content-Type", contentType);

      files.add(new MimeBodyPart(headers, content.getBytes()));

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

//  public HttpEntity build() {
//
//    MultipartEntityBuilder multiPart = MultipartEntityBuilder.create();
//    multiPart.addTextBody("form", form());
//
//    for(int i=0;i!=files.size();++i) {
//      multiPart.addPart("file" + i, files.get(i));
//    }
//
//    return multiPart.build();
//  }

  public MimeMultipart buildMultiPart() throws MessagingException, IOException {

    MimeMultipart multipart = new MimeMultipart();

    // Add the form data, encoded for reasons unknown as json
    InternetHeaders formHeaders = new InternetHeaders();
    formHeaders.addHeader("Content-Disposition", "form-data; name=\"json\"");
    multipart.addBodyPart(new MimeBodyPart(formHeaders, form()));

    // add the files
    for(MimeBodyPart file : files) {
      multipart.addBodyPart(file);
    }

    return multipart;
  }

  public byte[] form() {

    Map<String, Object> wrapper = new HashMap<>();
    wrapper.put("parameter", parameters);

    ObjectMapper mapper = new ObjectMapper();
    String json = null;
    try {
      json = mapper.writeValueAsString(wrapper);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return json.getBytes(Charsets.UTF_8);
  }
}
