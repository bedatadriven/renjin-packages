package org.renjin.ci.source.index;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.renjin.parser.RParser;
import org.renjin.sexp.ExpressionVector;
import org.renjin.sexp.SEXP;

import java.io.IOException;
import java.io.StringReader;


public class DefVisitorTest {

  @Test
  public void parseDefs() throws IOException {
    ExpressionVector sexp = parse();
    DefVisitor visitor = new DefVisitor();
    sexp.accept(visitor);
    
    System.out.println(visitor.getResult());
    
    Assert.assertThat(visitor.getResult(), Matchers.containsInAnyOrder(
        "readGAL",
        "strsplit2",
        "getLayout",
        "getDupSpacing",
        "getLayout2",
        "readTargets",
        "readSpotTypes",
        "controlStatus",
        "removeExt",
        "trimWhiteSpace",
        "protectMetachar",
        "spotc",
        "spotr",
        "gridc",
        "gridr",
        "printorder",
        "getSpacing"));
  }


  @Test
  public void parseUses() throws IOException {
    UseVisitor visitor = new UseVisitor();
    SEXP source = parse();
    source.accept(visitor);
    
    System.out.println(visitor.getResult());
  }
  
  private ExpressionVector parse() throws IOException {
    String source = Resources.toString(Resources.getResource(DefVisitor.class, "read.R"), Charsets.UTF_8);
    return RParser.parseAllSource(new StringReader(source));
  }
}