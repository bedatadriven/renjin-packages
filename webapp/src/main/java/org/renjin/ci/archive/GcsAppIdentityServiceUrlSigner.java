package org.renjin.ci.archive;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;


public class GcsAppIdentityServiceUrlSigner  {

  private static final int EXPIRATION_TIME = 5;
  private static final String BASE_URL = "https://storage.googleapis.com";


  private final AppIdentityService identityService = AppIdentityServiceFactory.getAppIdentityService();

  public String getSignedUrl(final String httpVerb, final String bucket, final String path) throws Exception {
    final long expiration = expiration();
    final String unsigned = stringToSign(expiration, bucket, path, httpVerb);
    final String signature = sign(unsigned);

    return new StringBuilder(BASE_URL).append("/")
        .append(bucket)
        .append("/")
        .append(path)
        .append("?GoogleAccessId=")
        .append(clientId())
        .append("&Expires=")
        .append(expiration)
        .append("&Signature=")
        .append(URLEncoder.encode(signature, "UTF-8")).toString();
  }

  private static long expiration() {
    final long unitMil = 1000l;
    final Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.MINUTE, EXPIRATION_TIME);
    final long expiration = calendar.getTimeInMillis() / unitMil;
    return expiration;
  }

  private String stringToSign(final long expiration, String bucket, String path, String httpVerb) {
    final String contentType = "";
    final String contentMD5 = "";
    final String canonicalizedExtensionHeaders = "";
    final String canonicalizedResource = "/" + bucket + "/" + path;
    final String stringToSign = httpVerb + "\n" + contentMD5 + "\n" + contentType + "\n"
        + expiration + "\n" + canonicalizedExtensionHeaders + canonicalizedResource;
    return stringToSign;
  }

  protected String sign(final String stringToSign) throws UnsupportedEncodingException {
    final AppIdentityService.SigningResult signingResult = identityService
        .signForApp(stringToSign.getBytes());
    final String encodedSignature = new String(Base64.encodeBase64(
        signingResult.getSignature(), false), "UTF-8");
    return encodedSignature;
  }

  protected String clientId() {
    return identityService.getServiceAccountName();
  }
}

