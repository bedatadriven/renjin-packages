package org.renjin.ci.packages;


import org.renjin.ci.datastore.Package;

public class ContributedPackagePage {
  private Package packageEntity;

  public ContributedPackagePage(Package packageEntity) {
    this.packageEntity = packageEntity;
  }
  
  public String getVersion() {
    return packageEntity.getLatestVersion();
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
    return xml.toString();
  }


  public String getRenjinLibraryCall() {
    return String.format("library('%s:%s')", packageEntity.getGroupId(), packageEntity.getName());
  }
  
}
