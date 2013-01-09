package net.adamcin.recap.impl.servlet;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import net.adamcin.recap.RecapStrategyException;
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

        RecapSession recapSession = request.adaptTo(RecapSession.class);
        if (recapSession != null) {
            recapSession.setTracker(response.adaptTo(ProgressTrackerListener.class));
            try {
                recapSession.doCopy();
            } catch (RecapSessionException e) {
                LOGGER.error("[doPost] Failed to copy paths");
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (RecapStrategyException e) {
                LOGGER.error("[doPost] Failed to list paths on remote server");
                response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            LOGGER.error("[doPost] Failed to adapt request to RecapSession");
        }

    }
}
