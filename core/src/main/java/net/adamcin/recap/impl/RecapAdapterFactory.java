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

package net.adamcin.recap.impl;

import net.adamcin.recap.api.*;
import net.adamcin.recap.util.DefaultProgressListener;
import net.adamcin.recap.util.DefaultRequestDepthConfig;
import net.adamcin.recap.util.HtmlProgressListener;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.io.IOException;

/**
 * @author madamcin
 * @version $Id: RecapAdapterFactory.java$
 */
@Component
@Service
@Properties({
        @Property(name = SlingConstants.PROPERTY_ADAPTABLE_CLASSES,
                classValue = {
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class
                }
        ),
        @Property(name = SlingConstants.PROPERTY_ADAPTER_CLASSES,
                classValue = {
                        RecapAddress.class,
                        RecapOptions.class,
                        RecapSession.class,
                        RecapProgressListener.class
                }
        )
})
public class RecapAdapterFactory implements AdapterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapAdapterFactory.class);

    @Reference
    private Recap recap;

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletResponse) {
            return getAdapter((SlingHttpServletResponse) adaptable, type);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(SlingHttpServletRequest adaptable, Class<AdapterType> type) {
        if (type == RecapSession.class) {

            RecapAddress recapAddress = getRecapAddress(adaptable);
            if (recapAddress != null) {
                RecapOptions recapOptions = getRecapOptions(adaptable);
                try {
                    RecapSession session = recap.initSession(adaptable.getResourceResolver().adaptTo(Session.class), recapAddress, recapOptions);

                    return (AdapterType) session;
                } catch (RecapSessionException e) {
                    LOGGER.error("failed to open recap session", e);
                }
            } else {
                LOGGER.error("remote host parameter not specified");
            }
        } else if (type == RecapOptions.class) {
            return (AdapterType) getRecapOptions(adaptable);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(SlingHttpServletResponse adaptable, Class<AdapterType> type) {
        if (type == RecapProgressListener.class) {
            if ("text/html".equals(adaptable.getContentType())) {
                try {
                    return (AdapterType) new HtmlProgressListener(adaptable.getWriter());
                } catch (IOException e) {
                    LOGGER.error("failed to adapt response to listener", e);
                }
            } else {
                try {
                    return (AdapterType) new DefaultProgressListener(adaptable.getWriter());
                } catch (IOException e) {
                    LOGGER.error("failed to adapt response to listener", e);
                }
            }
        }
        return null;
    }

    public RecapAddress getRecapAddress(SlingHttpServletRequest request) {
        String rpHost = request.getParameter(RecapConstants.RP_HOSTNAME);
        if (rpHost != null) {
            RecapAddressImpl address = new RecapAddressImpl();
            address.setHostname(rpHost);

            String rpPort = request.getParameter(RecapConstants.RP_PORT);

            if (StringUtils.isNotBlank(rpPort)) {
                try {
                    address.setPort(Integer.valueOf(rpPort));
                } catch (Exception e) {
                    LOGGER.error("[getAddress] failed to parse remote port parameter: " + rpPort, e);
                }
            }

            if ("true".equals(request.getParameter(RecapConstants.RP_IS_HTTPS))) {
                address.setHttps(true);
            }

            address.setUsername(request.getParameter(RecapConstants.RP_USERNAME));
            address.setPassword(request.getParameter(RecapConstants.RP_PASSWORD));
            address.setServletPath(request.getParameter(RecapConstants.RP_SERVLET_PATH));

            return address;
        }

        return null;
    }

    public RecapOptions getRecapOptions(SlingHttpServletRequest request) {
        RecapOptionsImpl options = new RecapOptionsImpl();

        if ("true".equals(request.getParameter(RecapConstants.RP_UPDATE))) {
            options.setUpdate(true);
        }

        if ("true".equals(request.getParameter(RecapConstants.RP_ONLY_NEWER))) {
            options.setOnlyNewer(true);
        }

        if ("true".equals(request.getParameter(RecapConstants.RP_REVERSE))) {
            options.setReverse(true);
        }

        if ("true".equals(request.getParameter(RecapConstants.RP_NO_RECURSE))) {
            options.setNoRecurse(true);
        }

        if ("true".equals(request.getParameter(RecapConstants.RP_NO_DELETE))) {
            options.setNoDelete(true);
        }

        if ("true".equals(request.getParameter(RecapConstants.RP_KEEP_ORDER))) {
            options.setKeepOrder(true);
        }

        String rpBatchSize = request.getParameter(RecapConstants.RP_BATCH_SIZE);
        if (StringUtils.isNotBlank(rpBatchSize)) {
            try {
                options.setBatchSize(Integer.valueOf(rpBatchSize));
            } catch (Exception e) {
                LOGGER.error("failed to parse batch_size parameter: " + rpBatchSize, e);
            }
        }

        String rpThrottle = request.getParameter(RecapConstants.RP_THROTTLE);
        if (StringUtils.isNotBlank(rpThrottle)) {
            try {
                options.setThrottle(Long.valueOf(rpThrottle));
            } catch (Exception e) {
                LOGGER.error("failed to parse throttle parameter: " + rpBatchSize, e);
            }
        }

        options.setLastModifiedProperty(request.getParameter(RecapConstants.RP_LAST_MODIFIED_PROPERTY));

        String rpBatchReadConfig = request.getParameter(RecapConstants.RP_REQUEST_DEPTH_CONFIG);
        if (StringUtils.isNotBlank(rpBatchReadConfig)) {
            options.setRequestDepthConfig(DefaultRequestDepthConfig.parseParameterValue(rpBatchReadConfig));
        }

        return options;
    }
}
