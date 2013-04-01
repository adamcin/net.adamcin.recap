package net.adamcin.recap.graniteit.remote.addressbook;

import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.commons.testing.sling.ResourceResolverTestBody;
import net.adamcin.recap.addressbook.AddressBook;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.apache.sling.junit.annotations.TestReference;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(SlingAnnotationsTestRunner.class)
public class AddressBookTest {

    // increment this number when more tests are added.
    public static final int NUMBER_OF_TESTS = 1;

    @TestReference
    ResourceResolverFactory resolverFactory;

    @Test
    public void testAdaptTo() {
        TestBody.test(new ResourceResolverTestBody() {
            @Override protected void execute() throws Exception {
                resolver = resolverFactory.getAdministrativeResourceResolver(null);

                AddressBook addressBook = resolver.adaptTo(AddressBook.class);

                assertNotNull("addressBook should not be null", addressBook);
            }
        });
    }
}
