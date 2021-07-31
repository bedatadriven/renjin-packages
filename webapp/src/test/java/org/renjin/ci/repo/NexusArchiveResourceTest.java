package org.renjin.ci.repo;

import junit.framework.TestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class NexusArchiveResourceTest extends TestCase {

    @Test
    public void test() {
        assertThat(NexusArchiveResource.filenameOf("org/apache/"), equalTo("apache/"));
        assertThat(NexusArchiveResource.filenameOf("org/apache/test.jar"), equalTo("test.jar"));
        assertThat(NexusArchiveResource.filenameOf("org"), equalTo("org"));
    }

}