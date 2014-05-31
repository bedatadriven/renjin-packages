package org.renjin.build.task;


import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class Bioconductor {
  
  public static List<PackageEntry> fetchPackageList() throws IOException {
    URL url = new URL("http://master.bioconductor.org/packages/json/2.12/bioc/packages.js");
    String json = Resources.toString(url, Charsets.UTF_8);

    int startIndex = json.indexOf('{');
    int endIndex = json.lastIndexOf(';');
    json = json.substring(startIndex, endIndex);
    

    ObjectMapper mapper = new ObjectMapper();
    JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead
    JsonParser jp = factory.createJsonParser(json);
    ArrayNode packageArray = (ArrayNode) mapper.readTree(jp).get("content");
    
//    List<PackageEntry> pkgList = Lists.newArrayList();
//    for(int i=0;i!=packageArray.size();++i) {
//      ArrayNode pkgNode = (ArrayNode) packageArray.get(i);
//      PackageEntry pkg = new PackageEntry(pkgNode.);
//
//
//    }
//

    throw new UnsupportedOperationException();
    
  }
  
}
