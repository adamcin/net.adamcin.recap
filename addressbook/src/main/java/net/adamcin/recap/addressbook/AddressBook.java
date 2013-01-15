package net.adamcin.recap.addressbook;

import org.apache.sling.api.resource.Resource;

/**
 * @author madamcin
 * @version $Id: AddressBook.java$
 */
public interface AddressBook {

    Resource getResource();

    Iterable<Address> listAddresses();
}
