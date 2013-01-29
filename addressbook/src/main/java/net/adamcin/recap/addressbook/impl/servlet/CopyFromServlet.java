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

package net.adamcin.recap.addressbook.impl.servlet;

import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapException;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapProgressListener;
import net.adamcin.recap.api.RecapSession;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: CopyFromServlet.java$
 */
@SlingServlet(resourceTypes = AddressBookConstants.RT_ADDRESS, methods = HttpConstants.METHOD_POST, selectors = AddressBookConstants.SELECTOR_COPY)
public class CopyFromServlet extends SlingAllMethodsServlet {

    @Reference
    private Recap recap;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        if ("html".equals(request.getRequestPathInfo().getExtension())) {
            response.setContentType("text/html");
        } else {
            response.setContentType("text/plain");
        }

        Address address = request.getResource().adaptTo(Address.class);
        if (address != null) {
            RecapOptions recapOptions = request.adaptTo(RecapOptions.class);

            try {
                RecapSession recapSession = recap.initSession(request.getResourceResolver().adaptTo(Session.class), address, recapOptions);
                recapSession.setProgressListener(response.adaptTo(RecapProgressListener.class));
                try {
                    List<String> paths = new ArrayList<String>();
                    String[] rpPaths = request.getParameterValues(AddressBookConstants.RP_PATHS);

                    if (rpPaths != null) {
                        for (String path : rpPaths) {
                            if (path.indexOf('\n') >= 0) {
                                String[] _paths = StringUtils.split(path, '\n');
                                for (String _path : _paths) {
                                    paths.add(_path.trim());
                                }
                            } else {
                                paths.add(path.trim());
                            }
                        }
                    }

                    for (String path : paths) {
                        if (StringUtils.isNotEmpty(path) && path.startsWith("/")) {
                            recapSession.sync(path);
                        }
                    }
                } finally {
                    recapSession.finish();
                }
            } catch (RecapException e) {
                throw new ServletException(e);
            }
        }
    }
}
