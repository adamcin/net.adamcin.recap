package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.addressbook.AddressBook;
import net.adamcin.recap.addressbook.AddressBookConstants;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;

import javax.servlet.http.HttpServletRequest;

/**
 * @author madamcin
 * @version $Id: NewAddressResourceDecorator.java$
 */
@Component
@Service
public class NewAddressResourceDecorator implements ResourceDecorator {

    public Resource decorate(Resource resource) {
        if (ResourceUtil.isStarResource(resource)) {
            Resource parent = resource.getParent();
            if (parent != null && parent.adaptTo(AddressBook.class) != null) {
                return new ResourceWrapper(resource) {
                    @Override
                    public String getResourceType() {
                        return AddressBookConstants.RT_ADDRESS;
                    }
                };
            }
        }
        return null;
    }

    public Resource decorate(Resource resource, HttpServletRequest request) {
        return decorate(resource);
    }
}
