package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import com.day.jcr.vault.util.RepositoryProvider;
import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapSessionContext;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import net.adamcin.recap.RecapSourceContext;
import net.adamcin.recap.RecapSourceException;
import net.adamcin.recap.RecapStrategy;
import net.adamcin.recap.RecapStrategyDescriptor;
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeService;
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

    @Reference
    private MetaTypeService metaTypeService;

    private int defaultRemotePort;
    private String defaultRemoteUser;
    private String defaultRemotePass;
    private RecapStrategyManager strategyManager;

    volatile boolean sessionsInterrupted = false;

    @Activate
    protected void activate(ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        defaultRemotePort = PropertiesUtil.toInteger(props.get(OSGI_DEFAULT_REMOTE_PORT), RecapConstants.DEFAULT_DEFAULT_REMOTE_PORT);
        defaultRemoteUser = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_USER), RecapConstants.DEFAULT_DEFAULT_REMOTE_USER);
        defaultRemotePass = PropertiesUtil.toString(props.get(OSGI_DEFAULT_REMOTE_PASS), RecapConstants.DEFAULT_DEFAULT_REMOTE_PASS);
        strategyManager = new RecapStrategyManager(ctx.getBundleContext(), metaTypeService);
        this.sessionsInterrupted = false;
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        this.sessionsInterrupted = true;
        defaultRemotePort = 0;
        defaultRemoteUser = null;
        defaultRemotePass = null;
        strategyManager = null;
    }

    public int getDefaultRemotePort() {
        return defaultRemotePort;
    }

    public String getDefaultRemoteUser() {
        return defaultRemoteUser;
    }

    public RecapSession initSession(ResourceResolver resourceResolver,
                                    RecapSessionContext context)
            throws RecapSessionException {

        RecapSessionContext remoteContext = applySessionContextDefaults(context);
        RecapSourceContext sourceContext = remoteContext.getSourceContext();
        Session localSession = resourceResolver.adaptTo(Session.class);
        Session srcSession;

        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            RepositoryAddress remoteAddress = getRemoteAddress(sourceContext);
            Repository srcRepo = this.getRepository(remoteAddress);
            srcSession = srcRepo.login(
                    new SimpleCredentials(sourceContext.getRemoteUsername(),
                            sourceContext.getRemotePassword().toCharArray()));
        } catch (Exception e) {
            throw new RecapSessionException("Failed to login to source repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }

        RecapSessionImpl recapSession = new RecapSessionImpl(this, remoteContext, localSession, srcSession);
        return recapSession;
    }

    private Repository getRepository(RepositoryAddress address) throws RepositoryException, URISyntaxException {
        RepositoryProvider repProvider = new RepositoryProvider();

        return repProvider.getRepository(address);
    }

    private InputStreamReader executeRequest(RecapSourceContext context, String getPath) throws IOException {

        RecapSourceContext contextWithDefaults = applySourceContextDefaults(context);

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

    public Iterator<RecapPath> listRemotePaths(RecapSessionContext context) throws RecapSourceException {
        RecapSessionContext contextWithDefaults = applySessionContextDefaults(context);
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
            reader = executeRequest(contextWithDefaults.getSourceContext(), pathBuilder.toString());
            List<RecapPath> paths = new ArrayList<RecapPath>();

            if (reader != null) {

                BufferedReader breader = new BufferedReader(reader);

                String line;
                while ((line = breader.readLine()) != null) {
                    RecapPath path = RecapPath.parse(line);
                    if (path != null) {
                        paths.add(path);
                    }
                }
            }

            return paths.iterator();
        } catch (IOException e) {
            throw new RecapSourceException("Failed to list remote paths.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public List<RecapStrategyDescriptor> listRemoteStrategies(RecapSourceContext context) throws RecapSourceException {
        List<RecapStrategyDescriptor> strategies = new ArrayList<RecapStrategyDescriptor>();
        try {
            InputStreamReader reader = executeRequest(context, RecapConstants.SERVLET_STRATEGIES_PATH);
            if (reader != null) {
                JSONArray array = CDL.toJSONArray(new JSONTokener(reader));

                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jo = array.optJSONObject(i);
                        if (jo != null) {
                            String type = jo.optString(RecapConstants.KEY_STRATEGY_TYPE);
                            if (type != null) {
                                RecapStrategyDescriptorImpl descriptor = new RecapStrategyDescriptorImpl();
                                descriptor.setType(type);
                                descriptor.setLabel(jo.optString(RecapConstants.KEY_STRATEGY_LABEL));
                                descriptor.setDescription(jo.optString(RecapConstants.KEY_STRATEGY_DESCRIPTION));
                                strategies.add(descriptor);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw new RecapSourceException(e);
        } catch (IOException e) {
            throw new RecapSourceException(e);
        }
        return strategies;
    }

    public List<RecapStrategyDescriptor> listLocalStrategies() {
        return this.strategyManager.listStrategyDescriptors();
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
        this.sessionsInterrupted = true;
    }

    public void clearSessionInterrupt() {
        this.sessionsInterrupted = false;
    }

    private RepositoryAddress getRemoteAddress(RecapSourceContext context) throws URISyntaxException {
        StringBuilder addressBuilder = new StringBuilder(context.isHttps() ? "https://" : "http://");
        addressBuilder.append(context.getRemoteUsername()).append(":").append(context.getRemotePassword()).append("@");
        addressBuilder.append(context.getRemoteHost());
        if (context.getRemotePort() != 80) {
            addressBuilder.append(":").append(context.getRemotePort());
        }
        if (StringUtils.isNotEmpty(context.getContextPath()) && !"/".equals(context.getContextPath())) {
            addressBuilder.append(context.getContextPath());
        }
        addressBuilder.append("/crx/-/jcr:root");
        return new RepositoryAddress(addressBuilder.toString());
    }

    private RecapSourceContext applySourceContextDefaults(final RecapSourceContext context) {
        final int port = context.getRemotePort() > 0 ?
                context.getRemotePort() : defaultRemotePort;
        final String user = context.getRemoteUsername() != null ?
                context.getRemoteUsername() : defaultRemoteUser;
        final String pass = context.getRemotePassword() != null ?
                context.getRemotePassword() : defaultRemotePass;

        RecapSourceContextImpl contextWithDefaults = new RecapSourceContextImpl();

        contextWithDefaults.setRemoteHost(context.getRemoteHost());
        contextWithDefaults.setHttps(context.isHttps());
        contextWithDefaults.setContextPath(context.getContextPath());
        contextWithDefaults.setRemotePort(port);
        contextWithDefaults.setRemoteUsername(user);
        contextWithDefaults.setRemotePassword(pass);

        return contextWithDefaults;
    }

    private RecapSessionContext applySessionContextDefaults(final RecapSessionContext context) {
        final String[] selectors = context.getSelectors() != null ?
                context.getSelectors() : new String[0];
        final String suffix = context.getSuffix() != null ?
                context.getSuffix() : "";
        final List<NameValuePair> parameters = context.getParameters() != null ?
                context.getParameters() : Collections.<NameValuePair>emptyList();

        RecapSourceContext sourceContext = applySourceContextDefaults(context.getSourceContext());
        RecapSessionContextImpl contextWithDefaults = new RecapSessionContextImpl(sourceContext);
        contextWithDefaults.setStrategy(context.getStrategy());
        contextWithDefaults.setSelectors(selectors);
        contextWithDefaults.setSuffix(suffix);
        contextWithDefaults.setParameters(parameters);

        return contextWithDefaults;
    }
}
