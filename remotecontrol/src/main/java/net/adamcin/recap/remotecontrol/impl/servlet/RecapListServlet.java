package net.adamcin.recap.remotecontrol.impl.servlet;

import net.adamcin.recap.remotecontrol.RecapPath;
import net.adamcin.recap.remotecontrol.RecapRemoteControl;
import net.adamcin.recap.remotecontrol.RecapStrategy;
import net.adamcin.recap.remotecontrol.RecapRemoteException;
import net.adamcin.recap.remotecontrol.RemoteControlConstants;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: RecapListServlet.java$
 */
@SlingServlet(paths = {RemoteControlConstants.SERVLET_LIST_PATH})
public class RecapListServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapListServlet.class);

    @Reference
    private RecapRemoteControl recapRemoteControl;

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        String strategyType = getStrategyType(request);
        RecapStrategy strategy = checkStrategy(strategyType);

        try {

            Iterator<Node> it = strategy.listNodes(request);

            if (it != null) {
                while (it.hasNext()) {
                    try {
                        Node node = it.next();
                        response.getWriter().println(RecapPath.build(node).getLeaf().encodePath());
                    } catch (RepositoryException e) {
                        LOGGER.error("[doGet] Failed to list node.", e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Caught IO exception. Stopping silently.", e);
        } catch (RecapRemoteException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } finally {
            recapRemoteControl.ungetStrategy(strategy);
        }
    }

    /**
     *
     * @param strategyType
     * @return
     * @throws ServletException
     */
    private RecapStrategy checkStrategy(String strategyType) throws ServletException {
        RecapStrategy strategy = recapRemoteControl.getStrategy(strategyType);
        if (strategy == null) {
            throw new ServletException("Failed to get RecapStrategy for type: " + strategyType);
        } else {
            return strategy;
        }
    }

    private String getStrategyType(SlingHttpServletRequest request) throws ServletException {
        return request.getParameter(RemoteControlConstants.RP_STRATEGY);
    }

}
