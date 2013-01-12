package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.util.DefaultProgressListener;
import com.day.jcr.vault.util.HtmlProgressListener;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapRequest;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.api.RecapSessionException;
import net.adamcin.recap.api.RecapUtil;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                        RecapRequest.class,
                        RecapOptions.class,
                        RecapSession.class,
                        ProgressTrackerListener.class
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
                RecapRequest recapRequest = getRecapRequest(adaptable);
                RecapOptions recapOptions = getRecapOptions(adaptable);
                try {
                    RecapSession session = recap.initSession(adaptable.getResourceResolver(), recapAddress, recapRequest, recapOptions);

                    return (AdapterType) session;
                } catch (RecapSessionException e) {
                    LOGGER.error("failed to open recap session", e);
                }
            } else {
                LOGGER.error("remote host parameter not specified");
            }
        } else if (type == RecapRequest.class) {
            return (AdapterType) getRecapRequest(adaptable);
        } else if (type == RecapOptions.class) {
            return (AdapterType) getRecapOptions(adaptable);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(SlingHttpServletResponse adaptable, Class<AdapterType> type) {
        if (type == ProgressTrackerListener.class) {
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
            address.setRemoteHost(rpHost);

            String rpPort = request.getParameter(RecapConstants.RP_PORT);

            if (rpPort != null) {
                try {
                    address.setRemotePort(Integer.valueOf(rpPort));
                } catch (Exception e) {
                    LOGGER.error("[getAddress] failed to parse remote port parameter: " + rpPort, e);
                }
            }

            if ("true".equals(request.getParameter(RecapConstants.RP_IS_HTTPS))) {
                address.setHttps(true);
            }

            address.setRemoteUsername(request.getParameter(RecapConstants.RP_USERNAME));
            address.setRemotePassword(request.getParameter(RecapConstants.RP_PASSWORD));
            address.setContextPath(request.getParameter(RecapConstants.RP_CONTEXT_PATH));

            return address;
        }

        return null;
    }

    public RecapRequest getRecapRequest(SlingHttpServletRequest request) {
        RecapRequestImpl context = new RecapRequestImpl();

        context.setStrategy(request.getParameter(RecapConstants.RP_STRATEGY));

        context.setSuffix(request.getRequestPathInfo().getSuffix());
        String rpSuffix = request.getParameter(RecapConstants.RP_SUFFIX);

        if (rpSuffix != null) {
            context.setSuffix(rpSuffix);
        }

        context.setSelectors(request.getRequestPathInfo().getSelectors());
        String[] rpSelectors = request.getParameterValues(RecapConstants.RP_SELECTORS);

        if (rpSelectors != null) {
            context.setSelectors(rpSelectors);
        }

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        List<NameValuePair> qsPairs;

        try {
            qsPairs = RecapUtil.parse(new URI(request.getRequestURI()), request.getCharacterEncoding());
            for (NameValuePair pair : qsPairs) {
                if (!pair.getName().startsWith(":")) {
                    pairs.add(pair);
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error("[getSessionContext] failed to parse request URI", e);
            qsPairs = Collections.emptyList();
        }

        Set<Map.Entry<String, RequestParameter[]>> entries = request.getRequestParameterMap().entrySet();
        if (entries != null) {
            for (Map.Entry<String, RequestParameter[]> entry : entries) {
                String name = entry.getKey();
                if (!name.startsWith(":")) {
                    RequestParameter[] rpValues = entry.getValue();
                    if (rpValues != null) {
                        for (RequestParameter rpValue : rpValues) {
                            if (rpValue.isFormField()) {
                                NameValuePair pair = new NameValuePair(name, rpValue.getString());
                                if (!qsPairs.contains(pair)) {
                                    pairs.add(pair);
                                }
                            }
                        }
                    }
                }
            }
        }

        context.setParameters(Collections.unmodifiableList(pairs));

        return context;
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
                options.setThrottle(Integer.valueOf(rpThrottle));
            } catch (Exception e) {
                LOGGER.error("failed to parse batch_size parameter: " + rpBatchSize, e);
            }
        }

        return options;
    }
}
