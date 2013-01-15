package net.adamcin.recap.impl;

import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapProgressListener;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.api.RecapSessionException;
import net.adamcin.recap.util.DefaultProgressListener;
import net.adamcin.recap.util.HtmlProgressListener;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
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

            if (rpPort != null) {
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
            address.setContextPath(request.getParameter(RecapConstants.RP_CONTEXT_PATH));

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

        String rpBatchSize = request.getParameter(RecapConstants.RP_BATCH_SIZE);
        if (rpBatchSize != null) {
            try {
                options.setBatchSize(Integer.valueOf(rpBatchSize));
            } catch (Exception e) {
                LOGGER.error("failed to parse batch_size parameter: " + rpBatchSize, e);
            }
        }

        String rpThrottle = request.getParameter(RecapConstants.RP_THROTTLE);
        if (rpThrottle != null) {
            try {
                options.setThrottle(Long.valueOf(rpThrottle));
            } catch (Exception e) {
                LOGGER.error("failed to parse batch_size parameter: " + rpBatchSize, e);
            }
        }

        options.setLastModifiedProperty(request.getParameter(RecapConstants.RP_LAST_MODIFIED_PROPERTY));

        return options;
    }
}
