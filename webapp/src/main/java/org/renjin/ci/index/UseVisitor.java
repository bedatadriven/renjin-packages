package org.renjin.ci.index;

import com.google.common.collect.Sets;
import org.renjin.sexp.*;

import java.util.HashSet;
import java.util.Set;

public class UseVisitor extends SexpVisitor<Set<String>> {


  public static final Set<String> IGNORE = Sets.newHashSet(
      "function", "if", "for", "repeat", "while", "break", "return",
      "next", "continue", "+", "-", "/", "*", "!", "(", "{",
      "$", "[", "[[", "&", "&&", "|", "||", ":", 
      "<-", "=",
      ">", ">=", "<", "<=", "==", "!=", "c");

  private Set<String> usedFunctions = new HashSet<>();

  @Override
  public void visit(ExpressionVector vector) {
    for (SEXP sexp : vector) {
      sexp.accept(this);
    }
  }

  @Override
  public Set<String> getResult() {
    return usedFunctions;
  }

  @Override
  public void visit(Closure closure) {
    closure.getBody().accept(this);
    closure.getFormals().accept(this);
  }

  @Override
  public void visit(FunctionCall call) {
    maybeIndexUse(call);
    call.getFunction().accept(this);
    call.getArguments().accept(this);
  }

  @Override
  public void visit(PairList.Node pairList) {
    for (PairList.Node node : pairList.nodes()) {
      node.getValue().accept(this);
    }
  }

  private void maybeIndexUse(FunctionCall call) {
    SEXP function = call.getFunction();
    if(function instanceof Symbol) {
      String functionName = ((Symbol) function).getPrintName();

      if(!IGNORE.contains(functionName)) {
        usedFunctions.add(functionName);
      }
    }
  }
}
