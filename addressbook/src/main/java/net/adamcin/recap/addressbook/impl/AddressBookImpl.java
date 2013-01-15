package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBook;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;

import java.util.Collections;
import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: AddressBookImpl.java$
 */
public class AddressBookImpl implements AddressBook {

    private final Resource resource;

    public AddressBookImpl(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public Iterable<Address> listAddresses() {
        return new Iterable<Address>() {
            public Iterator<Address> iterator() {
                if (resource != null) {
                    Iterator<Resource> children = resource.listChildren();

                    if (children != null) {
                        return ResourceUtil.adaptTo(children, Address.class);
                    }
                }
                return Collections.<Address>emptyList().iterator();
            }
        };
    }
}
