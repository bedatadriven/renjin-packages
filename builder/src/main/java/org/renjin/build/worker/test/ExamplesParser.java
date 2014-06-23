package org.renjin.build.worker.test;


import org.renjin.parser.RdParser;
import org.renjin.sexp.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExamplesParser extends SexpVisitor<String> {

  private StringBuilder code = new StringBuilder();

  private ExampleVisitor exampleVisitor = new ExampleVisitor();

  @Override
  public void visit(ListVector list) {
    if("\\examples".equals(getTagName(list))) {
      for(SEXP exp : list) {
        exp.accept(exampleVisitor);
      }
    }
    for(SEXP sexp : list) {
      if(sexp instanceof ListVector) {
        sexp.accept(this);
      }
    }
  }

  private String getTagName(SEXP sexp) {
    SEXP tag = sexp.getAttribute(Symbol.get("Rd_tag"));
    if(tag instanceof StringVector) {
      return ((StringVector) tag).getElementAsString(0);
    } else {
      return "";
    }
  }

  @Override
  public void visit(StringVector vector) {
  }

  @Override
  protected void unhandled(SEXP exp) {
    throw new UnsupportedOperationException(exp.toString());
  }

  private class ExampleVisitor extends SexpVisitor {

    @Override
    public void visit(ListVector list) {

    }

    @Override
    public void visit(StringVector vector) {
      if(getTagName(vector).equals("RCODE")) {
        for(String line : vector) {
          code.append(line);
        }
      }
    }
  }


  /**
   * Parses the examples from an *.Rd file
   *
   * @param file an *.Rd file
   * @return the text of the examples section
   * @throws java.io.IOException
   */
  public static String parseExamples(File file) throws IOException {
    try {
      FileReader reader = new FileReader(file);
      RdParser parser = new RdParser();
      SEXP rd = parser.R_ParseRd(reader, StringVector.valueOf(file.getName()), false);

      ExamplesParser examples = new ExamplesParser();
      rd.accept(examples);

      return examples.code.toString();
    } catch(Exception e) {
      System.err.println("WARNING: Failed to parse examples from " + file.getName() + ": " + e.getMessage());
      return "";
    }
  }
}