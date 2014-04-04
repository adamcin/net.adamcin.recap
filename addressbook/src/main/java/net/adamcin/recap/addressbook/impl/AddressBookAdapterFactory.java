/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.recap.addressbook.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
                        ResourceResolver.class,
                        SlingHttpServletRequest.class
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
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
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
    public <AdapterType> AdapterType getAdapter(SlingHttpServletRequest adaptable, Class<AdapterType> type) {
        if (type == RecapAddress.class || type == Address.class) {
            return (AdapterType) getAddressRequest(adaptable);
        } else if (type == AddressBook.class) {
            return (AdapterType) getAddressBook(adaptable.getResource());
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

    public Address getAddressRequest(SlingHttpServletRequest request) {
        
    	if (request != null) {
    		Resource resource = request.getResource();
	    	if (resource != null && ResourceUtil.isStarResource(resource) 
	    			&& ResourceUtil.isA(resource, AddressBookConstants.RT_ADDRESS)) {
	    		Map<String, Object> props = new HashMap<String, Object>();
	    		
	    		if (request.getParameter(AddressBookConstants.PROP_HOSTNAME) != null) {
	    			props.put(AddressBookConstants.PROP_HOSTNAME, request.getParameter(AddressBookConstants.PROP_HOSTNAME));	    			
	    		}
	    		
	    		if (request.getParameter(AddressBookConstants.PROP_PORT) != null) {
	    			props.put(AddressBookConstants.PROP_PORT, request.getParameter(AddressBookConstants.PROP_PORT));	    			
	    		}
	    		
	    		if (request.getParameter(AddressBookConstants.PROP_IS_HTTPS) != null) {
	    			props.put(AddressBookConstants.PROP_IS_HTTPS, request.getParameter(AddressBookConstants.PROP_IS_HTTPS));	    			
	    		}
	    		
	    		ValueMap properties = new ValueMapDecorator(props);
	            return new AddressImpl(resource, recap, properties);
	        }
	        return getAddressResource(resource);
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
                            addressBookNode.setProperty("sling:resourceType", AddressBookConstants.RT_ADDRESS_BOOK);
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
