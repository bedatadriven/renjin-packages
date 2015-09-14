package org.renjin.ci.source;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.PackageSource;

import java.util.List;


public class SourceResult {
  
  private PackageSource source;
  private String keyword;
  
  private List<Snippet> snippets = Lists.newArrayList();

  public SourceResult(PackageSource source, String keyword) {
    this.source = source;
    this.keyword = keyword;

    List<String> lines = source.parseLines();
    for(int i=0;i!=lines.size();++i) {
      if(lines.get(i).contains(keyword)) {
        snippets.add(new Snippet(lines, i));
      }
    }
  }

  public List<Snippet> getSnippets() {
    return snippets;
  }

  public PackageSource getSource() {
    return source;
  }
  
  public String getFilename() {
    return source.getFilename();
  }
  
  public String getPackageVersion() {
    return source.getPackageVersionId().getVersionString();
  }
}
