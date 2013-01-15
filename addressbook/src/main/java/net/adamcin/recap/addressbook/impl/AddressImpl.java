package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.addressbook.Address;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * @author madamcin
 * @version $Id: AddressImpl.java$
 */
public class AddressImpl implements Address {

    private final Resource resource;
    private final ValueMap properties;

    public AddressImpl(Resource resource) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        this.resource = resource;
        this.properties = resource.adaptTo(ValueMap.class);
    }

    public Resource getResource() {
        return resource;
    }

    public String getHostname() {
        return properties.get(AddressBookConstants.PROP_HOSTNAME, String.class);
    }

    public Integer getPort() {
        return properties.get(AddressBookConstants.PROP_PORT, Integer.class);
    }

    public boolean isHttps() {
        return properties.get(AddressBookConstants.PROP_IS_HTTPS, false);
    }

    public String getUsername() {
        return properties.get(AddressBookConstants.PROP_USERNAME, String.class);
    }

    public String getPassword() {
        return properties.get(AddressBookConstants.PROP_PASSWORD, String.class);
    }

    public String getContextPath() {
        return properties.get(AddressBookConstants.PROP_CONTEXT_PATH, String.class);
    }
}
