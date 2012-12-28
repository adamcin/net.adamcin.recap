package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.util.DefaultProgressListener;
import com.day.jcr.vault.util.HtmlProgressListener;
import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
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
                        Resource.class,
                        SlingHttpServletRequest.class,
                        SlingHttpServletResponse.class
                }
        ),
        @Property(name = SlingConstants.PROPERTY_ADAPTER_CLASSES,
                classValue = {
                        RecapPath.class,
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
        if (adaptable instanceof Resource) {
            return getAdapter((Resource) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletRequest) {
            return getAdapter((SlingHttpServletRequest) adaptable, type);
        } else if (adaptable instanceof SlingHttpServletResponse) {
            return getAdapter((SlingHttpServletResponse) adaptable, type);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(Resource adaptable, Class<AdapterType> type) {
        if (type == RecapPath.class) {
            Node node = adaptable.adaptTo(Node.class);
            if (node != null) {
                try {
                    return (AdapterType) RecapPath.build(node);
                } catch (RepositoryException e) {
                    LOGGER.error("failed to adapt Resource to RecapPath", e);
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType getAdapter(SlingHttpServletRequest adaptable, Class<AdapterType> type) {
        if (type == RecapSession.class) {

            String rpPort = adaptable.getParameter(RecapConstants.RP_REMOTE_PORT);
            int remotePort = RecapConstants.DEFAULT_DEFAULT_REMOTE_PORT;

            if (rpPort != null) {
                try {
                    remotePort = Integer.valueOf(rpPort);
                } catch (Exception e) {
                    LOGGER.error("failed to parse remote port parameter: " + rpPort, e);
                }
            }

            String rpUser = adaptable.getParameter(RecapConstants.RP_REMOTE_USER);
            String remoteUser = RecapConstants.DEFAULT_DEFAULT_REMOTE_USER;

            if (rpUser != null) {
                remoteUser = rpUser;
            }

            String rpPass = adaptable.getParameter(RecapConstants.RP_REMOTE_PASS);
            String remotePass = RecapConstants.DEFAULT_DEFAULT_REMOTE_PASS;

            if (rpPass != null) {
                remotePass = rpPass;
            }

            String rpHost = adaptable.getParameter(RecapConstants.RP_REMOTE_HOST);
            String remoteHost = null;

            if (rpHost != null) {
                remoteHost = rpHost;

                try {

                    RecapSession session = recap.initSession(
                            adaptable.getResourceResolver(),
                            remoteHost, remotePort,
                            remoteUser, remotePass);


                    return (AdapterType) session;

                } catch (RecapSessionException e) {
                    LOGGER.error("failed to open recap session", e);
                }
            } else {
                LOGGER.error("remote host parameter not specified");
            }
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
}
