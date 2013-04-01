package net.adamcin.recap.testing.itests.addressbook;

import net.adamcin.commons.testing.junit.TestBody;
import org.apache.sling.testing.tools.sling.SlingTestBase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

public class DummyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyTest.class);

    @Test
    public void testSystemProperties() {
        TestBody.test(new TestBody() {
            @Override
            protected void execute() throws Exception {
                Properties props = System.getProperties();

                assertTrue("test.server.url is not empty", props.containsKey(SlingTestBase.TEST_SERVER_URL_PROP));
            }
        });

    }
}
