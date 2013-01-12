package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import com.day.jcr.vault.util.RepositoryProvider;
import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapPath;
import net.adamcin.recap.api.RecapRemoteException;
import net.adamcin.recap.api.RecapRequest;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.api.RecapSessionException;
import net.adamcin.recap.api.RecapStrategy;
import net.adamcin.recap.api.RecapStrategyDescriptor;
import net.adamcin.recap.api.RecapUtil;
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

    @Property(label = "Default Remote Port", intValue = RecapConstants.DEFAULT_DEFAULT_PORT)
    private static final String OSGI_DEFAULT_PORT = "default.port";

    @Property(label = "Default Remote Username", value = RecapConstants.DEFAULT_DEFAULT_USERNAME)
    private static final String OSGI_DEFAULT_USERNAME = "default.username";

    @Property(label = "Default Remote Password", value = RecapConstants.DEFAULT_DEFAULT_PASSWORD)
    private static final String OSGI_DEFAULT_PASSWORD = "default.password";

    @Property(label = "Default Remote Context Path", value = RecapConstants.DEFAULT_DEFAULT_CONTEXT_PATH)
    private static final String OSGI_DEFAULT_CONTEXT_PATH = "default.contextPath";

    @Property(label = "Default Batch Size", intValue = RecapConstants.DEFAULT_DEFAULT_BATCH_SIZE)
    private static final String OSGI_DEFAULT_BATCH_SIZE = "default.batchSize";

    @Property(label = "Default Last Modified Property", value = RecapConstants.DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY)
    private static final String OSGI_DEFAULT_LAST_MODIFIED_PROPERTY = "default.lastModifiedProperty";

    @Reference
    private MetaTypeService metaTypeService;

    private int defaultPort;
    private String defaultContextPath;
    private String defaultUsername;
    private String defaultPassword;
    private int defaultBatchSize;
    private String defaultLastModifiedProperty;
    private RecapStrategyManager strategyManager;

    boolean sessionsInterrupted = false;

    @Activate
    protected void activate(ComponentContext ctx) {
        Dictionary<?, ?> props = ctx.getProperties();
        defaultPort = PropertiesUtil.toInteger(props.get(OSGI_DEFAULT_PORT), RecapConstants.DEFAULT_DEFAULT_PORT);
        defaultContextPath = PropertiesUtil.toString(props.get(OSGI_DEFAULT_CONTEXT_PATH), RecapConstants.DEFAULT_DEFAULT_CONTEXT_PATH);
        defaultUsername = PropertiesUtil.toString(props.get(OSGI_DEFAULT_USERNAME), RecapConstants.DEFAULT_DEFAULT_USERNAME);
        defaultPassword = PropertiesUtil.toString(props.get(OSGI_DEFAULT_PASSWORD), RecapConstants.DEFAULT_DEFAULT_PASSWORD);
        defaultBatchSize = PropertiesUtil.toInteger(props.get(OSGI_DEFAULT_BATCH_SIZE), RecapConstants.DEFAULT_DEFAULT_BATCH_SIZE);
        defaultLastModifiedProperty = PropertiesUtil.toString(props.get(OSGI_DEFAULT_LAST_MODIFIED_PROPERTY), RecapConstants.DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY);
        strategyManager = new RecapStrategyManager(ctx.getBundleContext(), metaTypeService);
        this.sessionsInterrupted = false;
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        this.sessionsInterrupted = true;
        defaultPort = 0;
        defaultUsername = null;
        defaultPassword = null;
        defaultBatchSize = 0;
        defaultLastModifiedProperty = null;
        strategyManager = null;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getDefaultUsername() {
        return defaultUsername;
    }

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public String getDefaultLastModifiedProperty() {
        return defaultLastModifiedProperty;
    }

    public String getDefaultContextPath() {
        return defaultContextPath;
    }

    public RecapSession initSession(ResourceResolver resourceResolver,
                                    RecapAddress address,
                                    RecapRequest request,
                                    RecapOptions options)
            throws RecapSessionException {

        if (address == null) {
            throw new NullPointerException("address");
        }

        if (StringUtils.isEmpty(address.getHostname())) {
            throw new IllegalArgumentException("address.getHostname() must not be empty");
        }

        RecapAddress addr = applyAddressDefaults(address);
        RecapRequest req = applyRequestDefaults(request);
        RecapOptions opts = applyOptionsDefaults(options);
        Session localSession = resourceResolver.adaptTo(Session.class);
        Session srcSession;

        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            RepositoryAddress repositoryAddress = getRepositoryAddress(addr);
            Repository srcRepo = this.getRepository(repositoryAddress);
            srcSession = srcRepo.login(
                    new SimpleCredentials(addr.getUsername(),
                            addr.getPassword().toCharArray()));
        } catch (Exception e) {
            throw new RecapSessionException("Failed to login to source repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }

        return new RecapSessionImpl(this, addr, req, opts, localSession, srcSession);
    }

    private Repository getRepository(RepositoryAddress address) throws RepositoryException, URISyntaxException {
        RepositoryProvider repProvider = new RepositoryProvider();

        return repProvider.getRepository(address);
    }

    private InputStreamReader executeRequest(RecapAddress address, String getPath) throws IOException {

        RecapAddress addr = applyAddressDefaults(address);

        String uri;
        if (StringUtils.isNotEmpty(addr.getContextPath())) {
            uri = addr.getContextPath() + getPath;
        } else {
            uri = getPath;
        }

        GetMethod method = new GetMethod(uri);

        HttpHost targetHost = new HttpHost(addr.getHostname(),
                addr.getPort(), addr.isHttps() ? Protocol.getProtocol("https") : Protocol.getProtocol("http"));

        HttpClient client = new HttpClient();

        client.getHostConfiguration().setHost(addr.getHostname(), addr.getPort());
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(addr.getHostname(), addr.getPort()),
                new UsernamePasswordCredentials(addr.getUsername(), addr.getPassword())
        );

        int result = client.executeMethod(method);
        if (result == 200) {
            return new InputStreamReader(method.getResponseBodyAsStream(), method.getResponseCharSet());
        } else {
            return null;
        }
    }

    public Iterator<RecapPath> listRemotePaths(RecapAddress address, RecapRequest request) throws RecapRemoteException {
        RecapAddress addr = applyAddressDefaults(address);
        RecapRequest req = applyRequestDefaults(request);
        StringBuilder pathBuilder = new StringBuilder(RecapConstants.SERVLET_LIST_PATH);
        if (req.getSelectors() != null) {
            for (String selector : req.getSelectors()) {
                pathBuilder.append(".").append(selector);
            }
        }
        pathBuilder.append(".txt");
        if (StringUtils.isNotEmpty(req.getSuffix())) {
            if (!req.getSuffix().startsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(req.getSuffix());
        }

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new NameValuePair(RecapConstants.RP_STRATEGY, req.getStrategy()));

        if (req.getParameters() != null) {
            pairs.addAll(req.getParameters());
        }

        pathBuilder.append("?").append(RecapUtil.format(pairs, RecapUtil.UTF_8));

        InputStreamReader reader = null;

        try {
            reader = executeRequest(addr, pathBuilder.toString());
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
            throw new RecapRemoteException("Failed to list remote paths.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public List<RecapStrategyDescriptor> listRemoteStrategies(RecapAddress context) throws RecapRemoteException {
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
            throw new RecapRemoteException(e);
        } catch (IOException e) {
            throw new RecapRemoteException(e);
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

    private RepositoryAddress getRepositoryAddress(RecapAddress recapAddress) throws URISyntaxException {
        StringBuilder addressBuilder = new StringBuilder(recapAddress.isHttps() ? "https://" : "http://");
        addressBuilder.append(recapAddress.getUsername()).append(":").append(recapAddress.getPassword()).append("@");
        addressBuilder.append(recapAddress.getHostname());
        if (recapAddress.getPort() != 80) {
            addressBuilder.append(":").append(recapAddress.getPort());
        }
        if (StringUtils.isNotEmpty(recapAddress.getContextPath()) && !"/".equals(recapAddress.getContextPath())) {
            addressBuilder.append(recapAddress.getContextPath());
        }
        addressBuilder.append("/crx/-/jcr:root");
        return new RepositoryAddress(addressBuilder.toString());
    }

    private RecapAddress applyAddressDefaults(final RecapAddress address) {
        RecapAddressImpl dAddress= new RecapAddressImpl();

        dAddress.setRemotePort(defaultPort);
        dAddress.setRemoteUsername(defaultUsername);
        dAddress.setRemotePassword(defaultPassword);
        dAddress.setContextPath(defaultContextPath);

        if (address != null) {

            dAddress.setRemoteHost(address.getHostname());
            dAddress.setHttps(address.isHttps());
            dAddress.setContextPath(address.getContextPath());

            if (address.getPort() > 0) {
                dAddress.setRemotePort(address.getPort());
            }
            if (address.getUsername() != null) {
                dAddress.setRemoteUsername(address.getUsername());
            }
            if (address.getPassword() != null) {
                dAddress.setRemotePassword(address.getPassword());
            }
            if (address.getContextPath() != null) {
                dAddress.setContextPath(address.getContextPath());
            }
        }

        return dAddress;
    }

    private RecapRequest applyRequestDefaults(final RecapRequest request) {
        RecapRequestImpl dRequest= new RecapRequestImpl();

        dRequest.setStrategy(RecapConstants.DIRECT_STRATEGY);
        dRequest.setSelectors(new String[0]);
        dRequest.setSuffix("");
        dRequest.setParameters(Collections.<NameValuePair>emptyList());

        if (request != null) {
            if (request.getStrategy() != null) {
                dRequest.setStrategy(request.getStrategy());
            }
            if (request.getSelectors() != null) {
                dRequest.setSelectors(request.getSelectors());
            }
            if (request.getSuffix() != null) {
                dRequest.setSuffix(request.getSuffix());
            }
            if (request.getParameters() != null) {
                dRequest.setParameters(request.getParameters());
            }
        }

        return dRequest;
    }

    private RecapOptions applyOptionsDefaults(final RecapOptions options) {
        RecapOptionsImpl dOptions = new RecapOptionsImpl();
        dOptions.setBatchSize(defaultBatchSize);
        dOptions.setLastModifiedProperty(defaultLastModifiedProperty);

        if (options != null) {
            dOptions.setUpdate(options.isUpdate());
            dOptions.setOnlyNewer(options.isOnlyNewer());
            dOptions.setThrottle(options.getThrottle());
            if (options.getBatchSize() > 0) {
                dOptions.setBatchSize(options.getBatchSize());
            }
            if (options.getLastModifiedProperty() != null) {
                dOptions.setLastModifiedProperty(options.getLastModifiedProperty());
            }
        }

        return dOptions;
    }
}
