package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import com.day.jcr.vault.util.RepositoryProvider;
import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapRemoteContext;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import net.adamcin.recap.RecapStrategy;
import net.adamcin.recap.RecapStrategyException;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.http.NameValuePair;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author madamcin
 * @version $Id: RecapImpl.java$
 */
@Component(label = "Recap Service", metatype = true)
@Service
public class RecapImpl implements Recap {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecapImpl.class);

    @Property(label = "Default Remote Port", intValue = RecapConstants.DEFAULT_DEFAULT_REMOTE_PORT)
    private static final String OSGI_DEFAULT_REMOTE_PORT = "default.port";

    @Property(label = "Default Remote User", value = RecapConstants.DEFAULT_DEFAULT_REMOTE_USER)
    private static final String OSGI_DEFAULT_REMOTE_USER = "default.user";

    @Property(label = "Default Remote Password", value = RecapConstants.DEFAULT_DEFAULT_REMOTE_PASS)
    private static final String OSGI_DEFAULT_REMOTE_PASS = "default.pass";

    private int defaultRemotePort;
    private String defaultRemoteUser;
    private String defaultRemotePass;
    private RecapStrategyManager strategyManager;

    @Activate
    protected void activate(ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        defaultRemotePort = PropertiesUtil.toInteger(props.get(OSGI_DEFAULT_REMOTE_PORT), RecapConstants.DEFAULT_DEFAULT_REMOTE_PORT);
        defaultRemoteUser = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_USER), RecapConstants.DEFAULT_DEFAULT_REMOTE_USER);
        defaultRemotePass = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_PASS), RecapConstants.DEFAULT_DEFAULT_REMOTE_PASS);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        defaultRemotePort = 0;
        defaultRemoteUser = null;
        defaultRemotePass = null;
    }

    public RecapSession initSession(ResourceResolver resourceResolver,
                                    RecapRemoteContext context)
            throws RecapSessionException {

        RecapRemoteContext remoteContext = applyDefaults(context);
        Session localSession = resourceResolver.adaptTo(Session.class);
        Session srcSession;

        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            RepositoryAddress remoteAddress = getRemoteAddress(remoteContext);
            Repository srcRepo = this.getRepository(remoteAddress);
            srcSession = srcRepo.login();
        } catch (Exception e) {
            throw new RecapSessionException("Failed to login to source repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }

        return new RecapSessionImpl(this, remoteContext, localSession, srcSession);
    }

    private Repository getRepository(RepositoryAddress address) throws RepositoryException, URISyntaxException {
        RepositoryProvider repProvider = new RepositoryProvider();

        return repProvider.getRepository(address);
    }

    public Iterator<RecapPath> listRemotePaths(RecapRemoteContext context) throws RecapStrategyException {
        return null;
    }

    public List<String> listRemoteStrategies(RecapRemoteContext context) throws RecapStrategyException {
        return null;
    }

    public List<String> listLocalStrategies() {
        return null;
    }

    public RecapStrategy getStrategy(String strategyType) {
        return null;
    }

    public void ungetStrategy(RecapStrategy strategy) {
    }

    public void interruptSessions() {
    }

    public void clearSessionInterrupt() {
    }

    private RepositoryAddress getRemoteAddress(RecapRemoteContext context) throws URISyntaxException {
        StringBuilder addressBuilder = new StringBuilder("http://");
        addressBuilder.append(context.getRemoteUsername()).append(":").append(context.getRemotePassword()).append("@");
        addressBuilder.append(context.getRemoteHost());
        if (context.getRemotePort() != 80) {
            addressBuilder.append(":").append(context.getRemotePort());
        }
        addressBuilder.append("/crx/-/jcr:root");
        return new RepositoryAddress(addressBuilder.toString());
    }

    private RecapRemoteContext applyDefaults(final RecapRemoteContext context) {
        final String host = context.getRemoteHost();
        final int port = context.getRemotePort() > 0 ?
                context.getRemotePort() : defaultRemotePort;
        final String user = context.getRemoteUsername() != null ?
                context.getRemoteUsername() : defaultRemoteUser;
        final String pass = context.getRemotePassword() != null ?
                context.getRemotePassword() : defaultRemotePass;
        final String[] selectors = context.getSelectors() != null ?
                context.getSelectors() : new String[0];
        final String suffix = context.getSuffix() != null ?
                context.getSuffix() : "";
        final List<NameValuePair> parameters = context.getParameters() != null ?
                context.getParameters() : Collections.<NameValuePair>emptyList();

        return new RecapRemoteContext() {
            public String getRemoteHost() {
                return host;
            }

            public int getRemotePort() {
                return port;
            }

            public String getRemoteUsername() {
                return user;
            }

            public String getRemotePassword() {
                return pass;
            }

            public String getStrategy() {
                return context.getStrategy();
            }

            public String[] getSelectors() {
                return selectors;
            }

            public String getSuffix() {
                return suffix;
            }

            public List<NameValuePair> getParameters() {
                return parameters;
            }
        };
    }
}
