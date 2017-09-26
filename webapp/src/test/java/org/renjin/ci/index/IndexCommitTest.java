package org.renjin.ci.index;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.renjin.ci.AbstractDatastoreTest;
import org.renjin.ci.datastore.RenjinCommit;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

@Ignore
public class IndexCommitTest extends AbstractDatastoreTest {

  @Test
  public void testFetch() throws IOException {
   // https://raw.githubusercontent.com/bedatadriven/renjin/9f5566e4431ec99f0fe5ff70e48ec7a18c74db14/core/pom.xml

    RenjinCommit commit = IndexCommit.fetchCommit("9f5566e4431ec99f0fe5ff70e48ec7a18c74db14");
    assertThat(commit.getMessage(), equalTo("matrix(nrow=10^8, ncol=1) does no longer allocate huge NA vector"));
    assertThat(commit.getParents(), hasSize(1));
    assertThat(commit.getParents().get(0).getKey().getName(), equalTo("9e1869f014a170096e8c253538ab1bd1fe4d217f"));
    assertThat(commit.getCommitDate(), equalTo(DateTime.parse("2014-12-04T13:35:25Z").toDate()));
  }

  @Test
  public void testFetchRelease() throws IOException {
    RenjinCommit commit = IndexCommit.fetchCommit("cbf6435939168a0a527bdf97580bdb1f3f2ea264");
    assertThat(commit.getRelease().getKey().getName(), equalTo("0.7.0-RC7"));

  }

}