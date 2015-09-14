package org.renjin.ci.index;

import org.renjin.sexp.*;

import java.util.HashSet;
import java.util.Set;


public class DefVisitor extends SexpVisitor<Set<String>> {

  public static final Symbol FUNCTION = Symbol.get("function");
  public static final Symbol ARROW_ASSIGN = Symbol.get("<-");
  public static final Symbol EQUAL_ASSIGN = Symbol.get("=");
  
  private Set<String> declaredFunctions = new HashSet<>();

  @Override
  public void visit(ExpressionVector vector) {
    for (SEXP sexp : vector) {
      sexp.accept(this);
    }
  }

  @Override
  public void visit(FunctionCall call) {
    String declaredFunction = isFunctionDeclaration(call);
    if(declaredFunction != null) {
      declaredFunctions.add(declaredFunction);    
    } 
  }
  
  private boolean isAssignment(FunctionCall call) {
    return call.getFunction() == ARROW_ASSIGN || call.getFunction() == EQUAL_ASSIGN;
  }

  private String isFunctionDeclaration(FunctionCall call) {
    if(isAssignment(call)) {
      if(call.getArguments().length() == 2) {
        SEXP lhs = call.getArgument(0);
        SEXP rhs = call.getArgument(1);
        
        if(lhs instanceof Symbol && rhs instanceof FunctionCall && ((FunctionCall) rhs).getFunction() == FUNCTION) {
          return ((Symbol) lhs).getPrintName();  
        }
      }
    }
    return null;
  }

  @Override
  public Set<String> getResult() {
    return declaredFunctions;
  }
}
