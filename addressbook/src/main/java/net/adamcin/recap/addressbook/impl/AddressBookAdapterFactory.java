package net.adamcin.recap.addressbook.impl;

import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBook;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;


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
                        Address.class,
                        AddressBook.class
                }
        )
})
public class AddressBookAdapterFactory implements AdapterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressBookAdapterFactory.class);

    @Reference
    private Recap recap;

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        } else if (adaptable instanceof ResourceResolver) {
            return getAdapter((ResourceResolver) adaptable, type);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Resource adaptable, Class<AdapterType> type) {
        if (type == RecapAddress.class || type == Address.class) {
            return (AdapterType) getAddressResource(adaptable);
        } else if (type == AddressBook.class) {
            return (AdapterType) getAddressBook(adaptable);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(ResourceResolver adaptable, Class<AdapterType> type) {
        if (type == AddressBook.class) {
            return (AdapterType) getAddressBook(adaptable);
        }
        return null;
    }

    public AddressBook getAddressBook(Resource addressBookResource) {
        if (addressBookResource != null && ResourceUtil.isA(addressBookResource, AddressBookConstants.RT_ADDRESS_BOOK)) {
            return new AddressBookImpl(addressBookResource);
        }
        return null;
    }

    public Address getAddressResource(Resource resource) {
        if (resource != null && ResourceUtil.isA(resource, AddressBookConstants.RT_ADDRESS)) {
            return new AddressImpl(resource, recap);
        }
        return null;
    }

    public AddressBook getAddressBook(ResourceResolver resolver) {
        User user = resolver.adaptTo(User.class);
        if (user != null) {
            try {
                Resource userResource = resolver.getResource(user.getPath());
                if (userResource != null) {
                    Resource addressBookResource = userResource.getChild(AddressBookConstants.NN_ADDRESS_BOOK);
                    if (addressBookResource == null) {
                        Node userNode = userResource.adaptTo(Node.class);
                        if (userNode != null) {
                            Node addressBookNode = userNode.addNode(AddressBookConstants.NN_ADDRESS_BOOK, "sling:Folder");
                            addressBookNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, AddressBookConstants.RT_ADDRESS_BOOK);
                            userNode.getSession().save();
                            addressBookResource = resolver.getResource(addressBookNode.getPath());
                        }
                    }

                    if (addressBookResource != null) {
                        return getAddressBook(addressBookResource);
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error("[getAddressBook] encountered exception", e);
            }
        }
        LOGGER.debug("[getAddressBook] failed to get address book for user: {}", resolver.getUserID());
        return null;
    }

}
