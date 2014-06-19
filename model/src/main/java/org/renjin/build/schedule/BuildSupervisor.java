package org.renjin.build.schedule;


import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.renjin.build.PersistenceUtil;
import org.renjin.build.model.RPackageDependency;
import org.renjin.build.model.RPackageVersion;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 *
 */
public class BuildSupervisor {


  private EntityManager em;

  public BuildSupervisor() {
    em = PersistenceUtil.createEntityManager();
  }

}
