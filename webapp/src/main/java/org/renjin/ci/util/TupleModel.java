package org.renjin.ci.util;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import javax.persistence.Tuple;

public class TupleModel implements TemplateHashModel {

  private final Tuple tuple;

  public TupleModel(Tuple tuple) {
    this.tuple = tuple;
  }

  @Override
  public TemplateModel get(String s) throws TemplateModelException {
    return ObjectWrapper.DEFAULT_WRAPPER.wrap(tuple.get(s));
  }

  @Override
  public boolean isEmpty() throws TemplateModelException {
    return false;
  }
}
