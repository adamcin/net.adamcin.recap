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

import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.api.Recap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

/**
 * @author madamcin
 * @version $Id: AddressImpl.java$
 */
public class AddressImpl implements Address {

    private final Resource resource;
    private final ValueMap properties;
    private final Recap recap;

    public AddressImpl(Resource resource, Recap recap) {
        if (resource == null) {
            throw new NullPointerException("resource");
        }
        this.resource = resource;
        this.properties = resource.adaptTo(ValueMap.class);
        this.recap = recap;
    }

    public Resource getResource() {
        return resource;
    }

    public String getTitle() {
        return properties.get("jcr:title", String.class);
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

    @Override
    public String toString() {
        return recap.getDisplayableUrl(this);
    }
}
