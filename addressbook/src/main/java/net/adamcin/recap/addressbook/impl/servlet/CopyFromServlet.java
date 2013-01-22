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
                            recapSession.remoteCopy(path);
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
