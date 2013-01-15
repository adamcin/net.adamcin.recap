package net.adamcin.recap.addressbook.impl.servlet;

import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapException;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapProgressListener;
import net.adamcin.recap.api.RecapSession;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.IOException;

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

        Address address = request.getResource().adaptTo(Address.class);
        if (address != null) {
            RecapOptions recapOptions = request.adaptTo(RecapOptions.class);

            try {
                RecapSession recapSession = recap.initSession(request.getResourceResolver().adaptTo(Session.class), address, recapOptions);
                recapSession.setProgressListener(response.adaptTo(RecapProgressListener.class));
                try {
                    String[] paths = request.getParameterValues(":paths");
                    if (paths != null) {
                        for (String path : paths) {
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
