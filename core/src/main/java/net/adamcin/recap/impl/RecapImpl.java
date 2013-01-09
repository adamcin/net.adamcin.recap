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
import net.adamcin.recap.RecapUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

    @Property(label = "Default Remote Strategy", value = RecapConstants.DEFAULT_DEFAULT_REMOTE_STRATEGY)
    private static final String OSGI_DEFAULT_REMOTE_STRATEGY = "default.strategy";

    private int defaultRemotePort;
    private String defaultRemoteUser;
    private String defaultRemotePass;
    private String defaultRemoteStrategy;
    private RecapStrategyManager strategyManager;

    @Activate
    protected void activate(ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        defaultRemotePort = PropertiesUtil.toInteger(props.get(OSGI_DEFAULT_REMOTE_PORT), RecapConstants.DEFAULT_DEFAULT_REMOTE_PORT);
        defaultRemoteUser = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_USER), RecapConstants.DEFAULT_DEFAULT_REMOTE_USER);
        defaultRemotePass = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_PASS), RecapConstants.DEFAULT_DEFAULT_REMOTE_PASS);
        defaultRemoteStrategy = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_PASS), RecapConstants.DEFAULT_DEFAULT_REMOTE_STRATEGY);
        strategyManager = new RecapStrategyManager(ctx.getBundleContext());
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        defaultRemotePort = 0;
        defaultRemoteUser = null;
        defaultRemotePass = null;
        defaultRemoteStrategy = null;
        strategyManager = null;
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
            srcSession = srcRepo.login(
                    new SimpleCredentials(remoteContext.getRemoteUsername(),
                            remoteContext.getRemotePassword().toCharArray()));
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

    private InputStreamReader executeRequest(RecapRemoteContext context, String getPath) throws IOException {

        RecapRemoteContext contextWithDefaults = applyDefaults(context);

        String uri;
        if (StringUtils.isNotEmpty(contextWithDefaults.getContextPath())) {
            uri = contextWithDefaults.getContextPath() + getPath;
        } else {
            uri = getPath;
        }

        GetMethod method = new GetMethod(uri);

        HttpHost targetHost = new HttpHost(contextWithDefaults.getRemoteHost(),
                contextWithDefaults.getRemotePort(), contextWithDefaults.isHttps() ? Protocol.getProtocol("https") : Protocol.getProtocol("http"));

        HttpClient client = new HttpClient();

        client.getHostConfiguration().setHost(contextWithDefaults.getRemoteHost(), contextWithDefaults.getRemotePort());
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(contextWithDefaults.getRemoteHost(), contextWithDefaults.getRemotePort()),
                new UsernamePasswordCredentials(contextWithDefaults.getRemoteUsername(), contextWithDefaults.getRemotePassword())
        );

        int result = client.executeMethod(method);
        if (result == 200) {
            return new InputStreamReader(method.getResponseBodyAsStream(), method.getResponseCharSet());
        } else {
            return null;
        }
    }

    public Iterator<RecapPath> listRemotePaths(RecapRemoteContext context) throws RecapStrategyException {
        RecapRemoteContext contextWithDefaults = applyDefaults(context);
        StringBuilder pathBuilder = new StringBuilder(RecapConstants.SERVLET_LIST_PATH);
        if (contextWithDefaults.getSelectors() != null) {
            for (String selector : contextWithDefaults.getSelectors()) {
                pathBuilder.append(".").append(selector);
            }
        }
        pathBuilder.append(".txt");
        if (StringUtils.isNotEmpty(contextWithDefaults.getSuffix())) {
            if (!contextWithDefaults.getSuffix().startsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(contextWithDefaults.getSuffix());
        }

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new NameValuePair(RecapConstants.RP_REMOTE_STRATEGY, contextWithDefaults.getStrategy()));

        if (contextWithDefaults.getParameters() != null) {
            pairs.addAll(contextWithDefaults.getParameters());
        }

        pathBuilder.append("?").append(RecapUtil.format(pairs, RecapUtil.UTF_8));

        InputStreamReader reader = null;

        try {
            reader = executeRequest(contextWithDefaults, pathBuilder.toString());
            List<RecapPath> paths = new ArrayList<RecapPath>();

            BufferedReader breader = new BufferedReader(reader);

            String line;
            while ((line = breader.readLine()) != null) {
                RecapPath path = RecapPath.parse(line);
                if (path != null) {
                    paths.add(path);
                }
            }

            return paths.iterator();
        } catch (IOException e) {
            throw new RecapStrategyException("Failed to list remote paths.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public List<String> listRemoteStrategies(RecapRemoteContext context) throws RecapStrategyException {
        return null;
    }

    public List<String> listLocalStrategies() {
        List<String> strategies = new ArrayList<String>();
        Set<String> keys = this.strategyManager.getAllFactoryProperties(null).keySet();
        strategies.addAll(keys);
        return Collections.unmodifiableList(strategies);
    }

    public RecapStrategy getStrategy(String strategyType) {
        if (StringUtils.isNotEmpty(strategyType)) {
            return this.strategyManager.newInstance(strategyType);
        }
        return null;
    }

    public void ungetStrategy(RecapStrategy strategy) {
        this.strategyManager.release(strategy);
    }

    public void interruptSessions() {
    }

    public void clearSessionInterrupt() {
    }

    private RepositoryAddress getRemoteAddress(RecapRemoteContext context) throws URISyntaxException {
        StringBuilder addressBuilder = new StringBuilder(context.isHttps() ? "https://" : "http://");
        addressBuilder.append(context.getRemoteUsername()).append(":").append(context.getRemotePassword()).append("@");
        addressBuilder.append(context.getRemoteHost());
        if (context.getRemotePort() != 80) {
            addressBuilder.append(":").append(context.getRemotePort());
        }
        addressBuilder.append("/crx/-/jcr:root");
        return new RepositoryAddress(addressBuilder.toString());
    }

    private RecapRemoteContext applyDefaults(final RecapRemoteContext context) {
        final int port = context.getRemotePort() > 0 ?
                context.getRemotePort() : defaultRemotePort;
        final String user = context.getRemoteUsername() != null ?
                context.getRemoteUsername() : defaultRemoteUser;
        final String pass = context.getRemotePassword() != null ?
                context.getRemotePassword() : defaultRemotePass;
        final String strategy = context.getStrategy() != null ?
                context.getStrategy() : defaultRemoteStrategy;
        final String[] selectors = context.getSelectors() != null ?
                context.getSelectors() : new String[0];
        final String suffix = context.getSuffix() != null ?
                context.getSuffix() : "";
        final List<NameValuePair> parameters = context.getParameters() != null ?
                context.getParameters() : Collections.<NameValuePair>emptyList();

        RecapRemoteContextImpl contextWithDefaults = new RecapRemoteContextImpl();
        contextWithDefaults.setRemoteHost(context.getRemoteHost());
        contextWithDefaults.setHttps(context.isHttps());
        contextWithDefaults.setRemotePort(port);
        contextWithDefaults.setRemoteUsername(user);
        contextWithDefaults.setRemotePassword(pass);
        contextWithDefaults.setStrategy(strategy);
        contextWithDefaults.setSelectors(selectors);
        contextWithDefaults.setSuffix(suffix);
        contextWithDefaults.setParameters(parameters);

        return contextWithDefaults;
    }
}
