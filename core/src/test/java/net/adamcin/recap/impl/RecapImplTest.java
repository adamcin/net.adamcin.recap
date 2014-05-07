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

package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapConstants;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RecapImplTest {

    @Test
    public void testGetDisplayableUrl() {

        RecapImpl recap = new RecapImpl();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RecapImpl.OSGI_DEFAULT_PORT, 999);
        recap.activate(null, props);

        RecapAddressImpl address = new RecapAddressImpl();

        address.setHostname("localhost");
        address.setPort(-1);

        assertEquals("basic displayable url default port 999", "http://localhost:999/server", recap.getDisplayableUrl(address));

        address.setHttps(true);
        assertEquals("https url default port 999", "https://localhost:999/server", recap.getDisplayableUrl(address));

        address.setPort(443);
        assertEquals("https url port 443", "https://localhost/server", recap.getDisplayableUrl(address));

        address.setPort(80);
        assertEquals("https url port 80", "https://localhost:80/server", recap.getDisplayableUrl(address));

        address.setHttps(false);
        assertEquals("http url port 80", "http://localhost/server", recap.getDisplayableUrl(address));

        address.setPort(443);
        assertEquals("http url port 443", "http://localhost:443/server", recap.getDisplayableUrl(address));
    }

    @Test
    public void testGetRepositoryUrl() {

        RecapImpl recap = new RecapImpl();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RecapImpl.OSGI_DEFAULT_PORT, 999);
        props.put(RecapImpl.OSGI_DEFAULT_SERVLET_PATH, "/test/prefix");
        recap.activate(null, props);

        RecapAddressImpl address = new RecapAddressImpl();

        address.setHostname("localhost");
        address.setPort(-1);

        assertEquals("basic repository url default port 999", "http://localhost:999/test/prefix", recap.getRepositoryUrl(address));

        address.setHttps(true);
        assertEquals("https url default port 999", "https://localhost:999/test/prefix", recap.getRepositoryUrl(address));

        address.setPort(443);
        assertEquals("https url port 443", "https://localhost/test/prefix", recap.getRepositoryUrl(address));

        address.setPort(80);
        assertEquals("https url port 80", "https://localhost:80/test/prefix", recap.getRepositoryUrl(address));

        address.setHttps(false);
        assertEquals("http url port 80", "http://localhost/test/prefix", recap.getRepositoryUrl(address));

        address.setPort(443);
        assertEquals("http url port 443", "http://localhost:443/test/prefix", recap.getRepositoryUrl(address));

        address.setServletPath("/someOtherContext");
        assertEquals("http url port 443", "http://localhost:443/someOtherContext", recap.getRepositoryUrl(address));

        address.setServletPath("/");
        assertEquals("http url port 443", "http://localhost:443/", recap.getRepositoryUrl(address));

        address.setServletPath("");
        assertEquals("http url port 443", "http://localhost:443/", recap.getRepositoryUrl(address));
    }
}
