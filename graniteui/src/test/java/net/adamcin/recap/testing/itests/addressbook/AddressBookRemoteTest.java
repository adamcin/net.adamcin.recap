package net.adamcin.recap.testing.itests.addressbook;

import net.adamcin.commons.testing.sling.VltpackServerSideTestBase;
import net.adamcin.recap.graniteit.remote.addressbook.AddressBookTest;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestParameters;
import org.apache.sling.junit.remote.testrunner.SlingRemoteTestRunner;
import org.apache.sling.junit.remote.testrunner.SlingTestsCountChecker;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(SlingRemoteTestRunner.class)
public class AddressBookRemoteTest
        extends VltpackServerSideTestBase
        implements SlingRemoteTestParameters, SlingTestsCountChecker {

    public String getJunitServletUrl() {
        return getServerBaseUrl() + SLING_JUNIT_SERVLET_PATH;
    }

    public String getTestClassesSelector() {
        return AddressBookTest.class.getName();
    }

    public String getTestMethodSelector() {
        return null;
    }

    public void checkNumberOfTests(int numberOfTestsExecuted) {
        Assert.assertEquals("expect a specific number of tests", AddressBookTest.NUMBER_OF_TESTS, numberOfTestsExecuted);
    }
}
