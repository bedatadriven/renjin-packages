package org.renjin.ci.source;


import com.google.common.escape.Escaper;
import com.google.common.html.HtmlEscapers;

import java.util.List;

public class Snippet {
  
  private List<String> lines;
  private int lineIndex;


  public Snippet(List<String> lines, int lineIndex) {
    this.lines = lines;
    this.lineIndex = lineIndex;
  }
  
  public String getLine() {
    return lines.get(lineIndex);
  }
  
  public int getLineNumber() {
    return lineIndex + 1;
  }
  
  public String getHtmlTable() {
    Escaper escaper = HtmlEscapers.htmlEscaper();

    StringBuilder sb = new StringBuilder();
    sb.append("<div class=\"source-wrapper\">");
    sb.append("<table class=\"source-listing\">");
    for(int i=Math.max(0, lineIndex-2);i<Math.min(lines.size(), lineIndex+2);++i) {
      sb.append("<tr><td class=\"ln\">").append(i+1).append("</td><td class=\"line\">")
          .append(escaper.escape(lines.get(i)))
          .append("</td></tr>");
    }
    sb.append("</table>");
    sb.append("</div>");
    return sb.toString();
  }
}
