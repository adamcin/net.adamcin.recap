package net.adamcin.recap.remotecontrol.impl;

import net.adamcin.recap.remotecontrol.RecapRequest;
import net.adamcin.recap.remotecontrol.RecapUtil;
import net.adamcin.recap.remotecontrol.RemoteControlConstants;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author madamcin
 * @version $Id: RemoteControlAdapterFactory.java$
 */
public class RemoteControlAdapterFactory implements AdapterFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteControlAdapterFactory.class);

    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletResponse) {
            return getAdapter((SlingHttpServletResponse) adaptable, type);
        }
        return null;
    }

    public RecapRequest getRecapRequest(SlingHttpServletRequest request) {
        RecapRequestImpl recapRequest = new RecapRequestImpl();

        recapRequest.setStrategy(request.getParameter(RemoteControlConstants.RP_STRATEGY));

        String rpSuffix = request.getParameter(RemoteControlConstants.RP_SUFFIX);

        if (rpSuffix != null) {
            recapRequest.setSuffix(rpSuffix);
        }

        List<String> selectors = new ArrayList<String>();
        String rpSelector0 = request.getParameter(RemoteControlConstants.RP_SELECTOR_0);
        if (rpSelector0 != null) {
            selectors.add(rpSelector0);
        }

        String rpSelector1 = request.getParameter(RemoteControlConstants.RP_SELECTOR_1);
        if (rpSelector1 != null) {
            selectors.add(rpSelector1);
        }

        String rpSelector2 = request.getParameter(RemoteControlConstants.RP_SELECTOR_2);
        if (rpSelector2 != null) {
            selectors.add(rpSelector2);
        }

        String rpSelector3 = request.getParameter(RemoteControlConstants.RP_SELECTOR_3);
        if (rpSelector3 != null) {
            selectors.add(rpSelector3);
        }

        String[] rpSelectors = request.getParameterValues(RemoteControlConstants.RP_SELECTORS);

        if (rpSelectors != null) {
            selectors.addAll(Arrays.asList(rpSelectors));
        }

        recapRequest.setSelectors(Collections.unmodifiableList(selectors));
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

        recapRequest.setParameters(Collections.unmodifiableList(pairs));

        return recapRequest;
    }

    public <AdapterType> AdapterType getAdapter(SlingHttpServletRequest adaptable, Class<AdapterType> type) {
        if (type == RecapRequest.class) {
            return (AdapterType) getRecapRequest(adaptable);
        }
        return null;
    }
}
