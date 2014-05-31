package org.renjin.build.task;

import org.renjin.sexp.*;

public class ExamplesParser extends SexpVisitor<String> {

  private StringBuilder code = new StringBuilder();

  @Override
  public void visit(ListVector list) {
    SEXP tag = list.getAttribute(Symbol.get("Rd_tag"));
    if(tag != Null.INSTANCE) {
      StringVector tagName = (StringVector)tag;
      if(tagName.getElementAsString(0).equals("\\examples")) {
        for(SEXP exp : list) {
          exp.accept(this);
        }
      }
    }
    for(SEXP sexp : list) {
      if(sexp instanceof ListVector) {
        sexp.accept(this);
      }
    }
  }

  @Override
  public void visit(StringVector vector) {
    for(String line : vector) {
      code.append(line);
    }
  }

  @Override
  protected void unhandled(SEXP exp) {
    throw new UnsupportedOperationException(exp.toString());
  }

  @Override
  public String getResult() {
    return code.toString();
  }
}
