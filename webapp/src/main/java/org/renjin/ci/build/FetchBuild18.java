package org.renjin.ci.build;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.renjin.ci.model.PackageVersionId;
import org.renjin.ci.storage.StorageKeys;
import org.w3c.dom.*;
import org.w3c.tidy.Tidy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by alex on 28-4-15.
 */
public class FetchBuild18 {

  public static void main(String[] args) throws IOException {

    Document document = parsePage("http://packages.renjin.org/index.html");

    NodeList links = document.getElementsByTagName("a");
    for(int i=0;i<links.getLength();++i) {
      Node item = links.item(i);
      String href = item.getAttributes().getNamedItem("href").getNodeValue();
      if(href.startsWith("packages/")) {
        String packageName = innerText(item);
        fetch(packageName);
      }
    }
    
  }

  public static void fetch(String packageName) throws IOException {
    try {
      Document document = parsePage("http://packages.renjin.org/packages/" + packageName + ".html");

      String packageVersion = getPackageVersion(document);

      String buildLog = getBuildLog(document);
      
      if(buildLog == null) {
        System.out.println(packageName + ": No log");
        
      } else {
        PackageVersionId packageVersionId = new PackageVersionId("org.renjin.cran", packageName, packageVersion);

        File file = new File("/home/alex/dev/logs/18/" + StorageKeys.buildLog(packageVersionId, 18));
        if (!file.getParentFile().exists()) {
          boolean created = file.getParentFile().mkdirs();
          if (!created) {
            throw new RuntimeException("Could not create " + file.getParentFile());
          }
        }

        Files.write(buildLog, file, Charsets.UTF_8);

        System.out.println(packageName + ": Wrote " + file);
      }
    } catch (Exception e) {
      System.out.println(packageName + ": Error: " + e.getMessage());
    }
  }

  private static String getPackageVersion(Document document) {
    String h1 = innerText(document.getElementsByTagName("h1").item(0)).trim();
    int versionStart = h1.indexOf(' ');
    if(versionStart == -1) {
      throw new RuntimeException("Unexpected header: " + h1);
    }
    return h1.substring(versionStart+1);
  }

  private static String getBuildLog(Document document) {
    NodeList blocks = document.getElementsByTagName("pre");
    for(int i=0;i!=blocks.getLength();++i) {
      Node item = blocks.item(i);
      String text = innerText(item);
      if(text.contains("Scanning for projects")) {
        return text;
      }
    }
    return null;
  }

  private static Document parsePage(String url) throws IOException {
    URL logUrl = new URL(url);
    Tidy tidy = new Tidy();
    tidy.setXHTML(false);
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);

    Document document;
    try(InputStream in = logUrl.openStream()) {
      document = tidy.parseDOM(in, null);
    }
    return document;
  }


  private static String innerText(Node item) {
    StringBuilder text = new StringBuilder();
    innerText(item, text);
    return text.toString().trim();
  }


  private static void innerText(Node item, StringBuilder text) {
    if(item instanceof CharacterData) {
      text.append(((CharacterData) item).getData());
    } else {
      NodeList children = item.getChildNodes();
      for(int i=0;i!=children.getLength();++i) {
        innerText(children.item(i), text);
      }
    }
  }



}
