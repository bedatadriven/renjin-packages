package org.renjin.ci.packages;


import org.renjin.ci.datastore.Package;

public class ReplacementVersionPage {
  private Package packageEntity;

  public ReplacementVersionPage(Package packageEntity) {
    this.packageEntity = packageEntity;
  }
  
  public String getVersion() {
    return packageEntity.getLatestReplacementVersion();
  }

  public String getPomReference() {
    StringBuilder xml = new StringBuilder();
    xml.append("<dependencies>\n");
    xml.append("  <dependency>\n");
    xml.append("    <groupId>").append(packageEntity.getGroupId()).append("</groupId>\n");
    xml.append("    <artifactId>").append(packageEntity.getName()).append("</artifactId>\n");
    xml.append("    <version>").append(getVersion()).append("</version>\n");
    xml.append("  </dependency>\n");
    xml.append("</dependencies>\n");
    xml.append("<repositories>\n");
    xml.append("  <repository>\n");
    xml.append("    <id>bedatadriven</id>\n");
    xml.append("    <name>bedatadriven public repo</name>\n");
    xml.append("    <url>https://nexus.bedatadriven.com/content/groups/public/</url>\n");
    xml.append("  </repository>\n");
    xml.append("</repositories>");
    return xml.toString();
  }


  public String getRenjinLibraryCall() {
    return String.format("library('%s:%s')", packageEntity.getGroupId(), packageEntity.getName());
  }
  
}
