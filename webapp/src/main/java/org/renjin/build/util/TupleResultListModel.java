package org.renjin.build.util;

import freemarker.template.*;

import javax.persistence.Tuple;
import java.util.List;

public class TupleResultListModel implements TemplateSequenceModel {

  private List<Tuple> list;

  public TupleResultListModel(List<Tuple> list) {
    this.list = list;
  }

  @Override
  public TemplateModel get(int i) throws TemplateModelException {
    return new TupleModel(list.get(i));
  }

  @Override
  public int size() throws TemplateModelException {
    return list.size();
  }

}
