package net.adamcin.recap.addressbook.impl.servlet;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import net.adamcin.recap.addressbook.Address;
import net.adamcin.recap.addressbook.AddressBookConstants;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapException;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapSession;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author madamcin
 * @version $Id: DoCopyServlet.java$
 */
@SlingServlet(resourceTypes = AddressBookConstants.RT_ADDRESS, methods = HttpConstants.METHOD_POST, selectors = AddressBookConstants.SELECTOR_COPY)
public class DoCopyServlet extends SlingAllMethodsServlet {

    @Reference
    private Recap recap;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        Address address = request.getResource().adaptTo(Address.class);
        if (address != null) {
            RecapOptions recapOptions = request.adaptTo(RecapOptions.class);

            try {
                RecapSession recapSession = recap.initSession(request.getResourceResolver(), address, recapOptions);
                recapSession.setTracker(response.adaptTo(ProgressTrackerListener.class));
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
