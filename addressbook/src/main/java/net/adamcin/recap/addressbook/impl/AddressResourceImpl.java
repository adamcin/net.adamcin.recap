package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.addressbook.AddressResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * @author madamcin
 * @version $Id: AddressResourceImpl.java$
 */
public class AddressResourceImpl implements AddressResource {

    private Resource resource;
    private ValueMap properties;

    public AddressResourceImpl(Resource resource) {
        this.resource = resource;
        this.properties = resource.adaptTo(ValueMap.class);
    }

    public Resource getResource() {
        return this.resource;
    }

    public String getHostname() {
        return properties.get(RecapConstants.PROP_REMOTE_HOST, String.class);
    }

    public int getPort() {
        return properties.get(RecapConstants.PROP_REMOTE_PORT, 0);
    }

    public boolean isHttps() {
        return properties.get(RecapConstants.PROP_REMOTE_IS_HTTPS, false);
    }

    public String getUsername() {
        return properties.get(RecapConstants.PROP_REMOTE_USER, String.class);
    }

    public String getPassword() {
        return properties.get(RecapConstants.PROP_REMOTE_PASS, String.class);
    }

    public String getContextPath() {
        return properties.get(RecapConstants.PROP_REMOTE_CONTEXT_PATH, String.class);
    }
}
