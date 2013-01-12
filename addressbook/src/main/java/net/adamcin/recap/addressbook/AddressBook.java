package net.adamcin.recap.addressbook;

import org.apache.sling.api.resource.Resource;

import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: AddressBook.java$
 */
public interface AddressBook {

    Resource getResource();

    Iterator<AddressResource> listAddresses();
}
