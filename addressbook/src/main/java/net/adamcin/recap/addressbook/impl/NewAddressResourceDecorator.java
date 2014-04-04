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

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import net.adamcin.recap.addressbook.AddressBook;
import net.adamcin.recap.addressbook.AddressBookConstants;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceDecorator;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

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
                    @Override public String getResourceType() {
                        return AddressBookConstants.RT_ADDRESS;
                    }
                    @SuppressWarnings("unchecked")
                    @Override
                    public <AdapterType> AdapterType adaptTo(
                    		Class<AdapterType> type) {
                    	if (type == ValueMap.class) {
                    		ValueMap props = super.adaptTo(ValueMap.class);
                    		if (props == null) {
                            	return (AdapterType) new ValueMapDecorator(Collections.<String, Object>emptyMap());
                            } else {
                            	return (AdapterType) props;
                            }
                    	}
                    	return super.adaptTo(type);
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
