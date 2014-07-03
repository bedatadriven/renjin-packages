package org.renjin.ci.tasks.cran;

import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.renjin.ci.storage.StorageKeys;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;
import org.w3c.tidy.Tidy;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class CRAN {

  private static final Logger LOGGER = Logger.getLogger(CRAN.class.getName());

  public static final String CRAN_MIRROR = "http://ftp.heanet.ie/mirrors/cran.r-project.org/";

  public static List<String> fetchUpdatedPackageList(LocalDate lastUpdate) {
    String indexUrl = CRAN_MIRROR + "web/packages/available_packages_by_date.html";
    LOGGER.info("Fetching from " + indexUrl);

    try {
      return parseUpdatedPackageList(lastUpdate, Resources.asByteSource(new URL(indexUrl)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String fetchLatestPackageVersion(String packageName) {
    try {
      String indexUrl = CRAN_MIRROR + "web/packages/" + packageName + "/index.html";
      System.out.println("Fetching from " + indexUrl);
      return parsePackageVersion( Resources.asByteSource(new URL(indexUrl)));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String parsePackageVersion(ByteSource source) throws IOException {
    Document dom = fetchAsDom(source);

    NodeList rows = dom.getElementsByTagName("tr");
    for(int i=0;i!=rows.getLength();++i) {
      Element row = (Element)rows.item(i);
      NodeList cells = row.getElementsByTagName("td");
      if(cells.getLength() >= 2) {
        String header = innerText(cells.item(0));
        String value = innerText(cells.item(1));

        if(header.equals("Version:")) {
          return value;
        }
      }
    }
    throw new RuntimeException("Failed to parse version from html file");
  }


  public static List<String> parseUpdatedPackageList(LocalDate lastUpdate, ByteSource source) throws IOException {
    Document dom = fetchAsDom(source);

    List<String> packages = Lists.newArrayList();

    DateTimeFormatter dateFormat = DateTimeFormat.forPattern("YYYY-MM-dd");

    NodeList rows = dom.getElementsByTagName("tr");

    LOGGER.info("Received " + rows.getLength() + " rows");

    for(int i=0;i!=rows.getLength();++i) {
      Element row = (Element)rows.item(i);
      NodeList cells = row.getElementsByTagName("td");
      if(cells.getLength() >= 2) {
        String date = innerText(cells.item(0));
        String name = innerText(cells.item(1));

        if(!(date.isEmpty() || name.isEmpty())) {
          try {
            LocalDate publicationDate = dateFormat.parseLocalDate(date);
            if(publicationDate.isBefore(lastUpdate)) {
              break;
            }

            packages.add(name);

          } catch(Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse date " + date);
          }
        }
      }
    }
    return packages;
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

  private static Document fetchAsDom(ByteSource source) throws IOException {
    Tidy tidy = new Tidy();
    tidy.setXHTML(false);
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);

    try(InputStream in = source.openStream()) {
      return tidy.parseDOM(in, null);
    }
  }

  public static URL sourceUrl(String packageName, String version) throws MalformedURLException {
    return new URL(CRAN.CRAN_MIRROR + "src/contrib/" + packageName + "_" + version + ".tar.gz");
  }
  
  public static URL archivedSourceUrl(String packageName, String version) throws MalformedURLException {
    return new URL(CRAN.CRAN_MIRROR + "src/contrib/Archive/" + packageName + "/" + packageName + "_" + version + ".tar.gz");
  }

  public static GcsFilename gcsFileName(String packageName, String version) {
    return new GcsFilename(StorageKeys.BUCKET_NAME,
        StorageKeys.packageSource("org.renjin.cran", packageName, version));
  }
  
  public static String packageId(String packageName) {
    return  "org.renjin.cran:" + packageName;
  }

  public static String packageVersionId(String packageName, String version) {
    return packageId(packageName) + ":" + version;
  }
}