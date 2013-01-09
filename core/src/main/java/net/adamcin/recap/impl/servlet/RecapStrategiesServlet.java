package net.adamcin.recap.impl.servlet;

import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapStrategy;
import net.adamcin.recap.RecapStrategyException;
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
import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapListServlet.java$
 */
@SlingServlet(paths = {RecapConstants.SERVLET_STRATEGIES_PATH})
public class RecapStrategiesServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapStrategiesServlet.class);

    @Reference
    private Recap recap;

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain");
        response.setCharacterEncoding("utf-8");

        List<String> strategies = recap.listLocalStrategies();

        for (String strategy : strategies) {
            response.getWriter().println(strategy);
        }
    }
}
