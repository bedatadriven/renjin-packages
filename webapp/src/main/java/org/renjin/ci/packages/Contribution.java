package org.renjin.ci.packages;

import org.renjin.ci.model.PackageVersionId;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Contribution {
    private final PackageVersionId pvid;
    private final String description;
    private final String url;

    public Contribution(PackageVersionId pvid, String description, String url) {
        this.pvid = pvid;
        this.description = description;
        this.url = url;
    }

    public static Contribution fetchFromMaven(PackageVersionId pvid) {
        String checkUrl = "https://repo1.maven.org/maven2/" + pvid.getGroupId().replace('.', '/') +
                "/" + pvid.getPackageName() + "/" + pvid.getVersionString() + "/" +
                pvid.getPackageName() + "-" + pvid.getVersionString() + ".pom";

        try {
            URL url = new URL(checkUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if(urlConnection.getResponseCode() != 200) {
                return null;
            }
            return parseContribution(pvid, urlConnection.getInputStream());
        } catch (Exception e) {
            return null;
        }
    }

    public PackageVersionId getPvid() {
        return pvid;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    private static Contribution parseContribution(PackageVersionId pvid, InputStream inputStream) throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(inputStream);
        doc.getDocumentElement().normalize();

        String description = doc.getElementsByTagName("description").item(0).getTextContent();
        String url = doc.getElementsByTagName("url").item(0).getTextContent();


        return new Contribution(pvid, description, url);
    }
}
