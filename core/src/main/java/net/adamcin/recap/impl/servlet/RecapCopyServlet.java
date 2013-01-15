package net.adamcin.recap.impl.servlet;

import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapException;
import net.adamcin.recap.api.RecapProgressListener;
import net.adamcin.recap.api.RecapSession;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author madamcin
 * @version $Id: RecapCopyServlet.java$
 */
@SlingServlet(paths = {RecapConstants.SERVLET_COPY_PATH})
public class RecapCopyServlet extends SlingAllMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapCopyServlet.class);

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response)
            throws ServletException, IOException {

        if ("html".equals(request.getRequestPathInfo().getExtension())) {
            response.setContentType("text/html");
        } else {
            response.setContentType("text/plain");
        }

        RecapSession recapSession = request.adaptTo(RecapSession.class);
        if (recapSession != null) {
            recapSession.setProgressListener(response.adaptTo(RecapProgressListener.class));
            try {
                try {
                    String[] paths = request.getParameterValues(":path");
                    if (paths != null) {
                        for (String path : paths) {
                            recapSession.remoteCopy(path);
                        }
                    }
                } finally {
                    recapSession.finish();
                }
            } catch (RecapException e) {
                LOGGER.error("[doPost] Failed to copy paths", e);
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            LOGGER.error("[doPost] Failed to adapt request to RecapSession");
            response.sendError(SlingHttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
