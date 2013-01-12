package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.addressbook.AddressBook;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.addressbook.AddressResource;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapRequest;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.impl.RecapAddressImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;


/**
 * @author madamcin
 * @version $Id: AddressBookAdapterFactory.java$
 */
@Component
@Service
@Properties({
        @Property(name = SlingConstants.PROPERTY_ADAPTABLE_CLASSES,
                classValue = {
                        Resource.class,
                        ResourceResolver.class
                }
        ),
        @Property(name = SlingConstants.PROPERTY_ADAPTER_CLASSES,
                classValue = {
                        RecapAddress.class,
                        AddressResource.class,
                        AddressBook.class
                }
        )
})
public class AddressBookAdapterFactory implements AdapterFactory {

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof Resource) {

        } else if (adaptable instanceof ResourceResolver) {

        }
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Resource adaptable, Class<AdapterType> type) {
        if (type == RecapAddress.class || type == AddressResource.class) {
            return (AdapterType) getAddressResource(adaptable);
        }
        return null;
    }


    public AddressResource getAddressResource(Resource resource) {
        AddressResourceImpl address = new AddressResourceImpl(resource);
        if (StringUtils.isNotEmpty(address.getHostname())) {
            return address;
        }
        return null;
    }

}
