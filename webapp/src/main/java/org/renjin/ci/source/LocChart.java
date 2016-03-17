package org.renjin.ci.source;

import com.google.common.collect.Lists;
import org.renjin.ci.datastore.Loc;
import org.renjin.ci.source.index.Language;

import java.util.List;

/**
 * View Model for LOC
 */
public class LocChart {

  private String label;

  public class Span {
    private String label;
    private String className;
    private long count;
    private double percentage;

    public Span(String label, String className, long count, double percentage) {
      this.label = label;
      this.className = className;
      this.count = count;
      this.percentage = percentage;
    }

    public String getLabel() {
      return label;
    }

    public String getClassName() {
      return className;
    }

    public String getMloc() {
      double mloc = ((double)count) / 1_000_000d;
      return String.format("%.2f", mloc);
    }
  }
  
  private List<Span> spans = Lists.newArrayList();
  
  public LocChart(String label, Loc loc) {
    this.label = label;
    spans.add(new Span("R", "bar-r", loc.getR(), loc.getProportion(Language.R)));
    spans.add(new Span("C", "bar-c", loc.getC(), loc.getProportion(Language.C)));
    spans.add(new Span("C++", "bar-cpp", loc.getCpp(), loc.getProportion(Language.CPP)));
    spans.add(new Span("Fortran", "bar-fortran", loc.getFortran(), loc.getProportion(Language.FORTRAN)));
  }

  public List<Span> getLanguages() {
    return spans;
  }

  public String getLabel() {
    return label;
  }
  
  public String getChartHtml() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div class=\"lang-bar\">");
    for (Span span : spans) {
      if(span.count > 0) {
        sb.append("<div class=\"").append(span.className).append("\" ")
            .append("title=\"").append(String.format("%d lines (%.0f%%)", span.count, span.percentage * 100d)).append("\" ")
            .append("style=\"width: ").append(String.format("%.0f%%", span.percentage * 100d)).append(";\">")
            .append(span.label)
            .append("</div>");
      }
    }
    sb.append("</div>");
    return sb.toString();
  }
}
