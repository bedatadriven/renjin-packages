package org.renjin.ci;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.After;
import org.junit.Before;
import org.renjin.ci.datastore.PackageDatabase;

public class AbstractDatastoreTest {

  private LocalServiceTestHelper helper;


  @Before
  public final void setUp() {
    LocalServiceTestConfig datastoreConfig = new LocalDatastoreServiceTestConfig()
        .setStoreDelayMs(0)
        .setApplyAllHighRepJobPolicy();
    helper = new LocalServiceTestHelper(datastoreConfig);
    helper.setUp();

    PackageDatabase.init();
  }

  @After
  public final void tearDown() {
    helper.tearDown();
  }
}
