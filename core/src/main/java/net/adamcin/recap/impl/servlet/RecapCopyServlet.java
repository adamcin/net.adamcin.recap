package net.adamcin.recap.impl.servlet;

import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapSession;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
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

    private static final String RP_INTERRUPT = ":interrupt";

    private volatile boolean interrupt = true;

    @Override
    protected void doPost(SlingHttpServletRequest request,
                          SlingHttpServletResponse response)
            throws ServletException, IOException {

        if(request.getParameter(RP_INTERRUPT) != null) {
            this.interrupt = "true".equals(request.getParameter(RP_INTERRUPT));

        } else {

            RecapSession recapSession = request.adaptTo(RecapSession.class);
            if (recapSession != null) {

                try {
                    final HttpClient client = this.getClient(rpHost, rpUser, rpPass, rpPort);
                    final GetMethod getMethod = this.getListMethod(request, strategyType);
                    Thread requesterThread = null;
            }
        }

    }
}
