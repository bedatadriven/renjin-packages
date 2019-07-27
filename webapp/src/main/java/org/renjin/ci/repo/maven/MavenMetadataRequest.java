package org.renjin.ci.repo.maven;

public class MavenMetadataRequest {

  private final String groupId;
  private final String artifactId;
  private final String filename;

  public MavenMetadataRequest(String groupId, String artifactId, String filename) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.filename = filename;
  }

  public static boolean matches(String path) {
    return path.endsWith("maven-metadata.xml") ||
            path.endsWith("maven-metadata.xml.md5") ||
            path.endsWith("maven-metadata.xml.sha1");
  }

  public static MavenMetadataRequest parse(String path) {
    // https://nexus.bedatadriven.com/content/groups/public/org/renjin/cran/survey/maven-metadata.xml

    String[] parts = path.split("/");
    int partIndex = parts.length - 1;

    String filename = parts[partIndex--];
    String artifactId = parts[partIndex--];
    StringBuilder groupId = new StringBuilder();
    for (int i = 0; i <= partIndex; i++) {
      if(i > 0) {
        groupId.append('.');
      }
      groupId.append(parts[i]);
    }
    return new MavenMetadataRequest(groupId.toString(), artifactId, filename);
  }
}
