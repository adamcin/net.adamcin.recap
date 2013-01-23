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
