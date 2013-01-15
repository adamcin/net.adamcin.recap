package net.adamcin.recap.remotecontrol.impl.servlet;

import net.adamcin.recap.remotecontrol.RecapRemoteControl;
import net.adamcin.recap.remotecontrol.RecapStrategyDescriptor;
import net.adamcin.recap.remotecontrol.RemoteControlConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapListServlet.java$
 */
@SlingServlet(paths = {RemoteControlConstants.SERVLET_STRATEGIES_PATH})
public class RecapStrategiesServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapStrategiesServlet.class);

    @Reference
    private RecapRemoteControl recapRemoteControl;

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        try {
            List<RecapStrategyDescriptor> strategies = recapRemoteControl.listLocalStrategies();

            JSONWriter jsonWriter = new JSONWriter(response.getWriter());
            jsonWriter.array();
            for (RecapStrategyDescriptor strategy : strategies) {
                if (StringUtils.isNotEmpty(strategy.getType())) {
                    jsonWriter.object();

                    jsonWriter.key(RemoteControlConstants.KEY_STRATEGY_TYPE).value(strategy.getType());

                    if (StringUtils.isNotEmpty(strategy.getLabel())) {
                        jsonWriter.key(RemoteControlConstants.KEY_STRATEGY_LABEL).value(strategy.getLabel());
                    }

                    if (StringUtils.isNotEmpty(strategy.getDescription())) {
                        jsonWriter.key(RemoteControlConstants.KEY_STRATEGY_DESCRIPTION).value(strategy.getDescription());
                    }

                    jsonWriter.endObject();
                }
            }
            jsonWriter.endArray();
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }
}
