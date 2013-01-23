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
