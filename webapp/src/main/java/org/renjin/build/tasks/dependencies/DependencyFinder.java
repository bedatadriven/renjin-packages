package org.renjin.build.tasks.dependencies;

import com.google.common.collect.Lists;
import org.renjin.build.model.PackageDescription;
import org.renjin.sexp.*;

import java.util.List;

public class DependencyFinder extends SexpVisitor<List<PackageDescription.PackageDependency>> {

  private List<PackageDescription.PackageDependency> dependencies = Lists.newArrayList();

  @Override
  public void visit(FunctionCall call) {
    if(call.getFunction() == Symbol.get("require") || call.getFunction() == Symbol.get("library")) {
      parseDependencies(call);
    } else {
      for(PairList node : call.getArguments().nodes()) {
        node.accept(this);
      }
    }
  }

  private void parseDependencies(FunctionCall call) {
    for(PairList.Node arg : call.getArguments().nodes()) {
      if(arg.hasTag() && arg.getTag() == Symbol.get("package")) {
        parseDependencyName(arg.getValue());
      }
    }

    // no named argument, find the first unnamed argument
    for(PairList.Node arg : call.getArguments().nodes()) {
      if(!arg.hasTag()) {
        parseDependencyName(arg.getValue());
        return;
      }
    }
  }

  private void parseDependencyName(SEXP value) {
    if(value instanceof Symbol) {
      dependencies.add(new PackageDescription.PackageDependency(((Symbol) value).getPrintName()));
    } else if(value instanceof StringVector) {
      dependencies.add(new PackageDescription.PackageDependency(((StringVector) value).getElementAsString(0)));
    }
  }

  @Override
  public void visit(ExpressionVector vector) {
    for(SEXP exp : vector) {
      exp.accept(this);
    }
  }

  @Override
  public List<PackageDescription.PackageDependency> getResult() {
    return dependencies;
  }
}
