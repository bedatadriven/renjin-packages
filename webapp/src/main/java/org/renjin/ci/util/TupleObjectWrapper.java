package org.renjin.ci.util;

import freemarker.template.ObjectWrapper;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

import javax.persistence.Tuple;

public class TupleObjectWrapper implements ObjectWrapper {
  @Override
  public TemplateModel wrap(Object o) throws TemplateModelException {
    if(o instanceof Tuple) {
      return new TupleModel((Tuple) o);
    } else {
      return ObjectWrapper.DEFAULT_WRAPPER.wrap(o);
    }
  }
}
