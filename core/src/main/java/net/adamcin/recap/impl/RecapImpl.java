package net.adamcin.recap.impl;

import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.api.RecapSessionException;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.client.RepositoryFactoryImpl;
import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.spi.commons.logging.Slf4jLogWriterProvider;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

/**
 * @author madamcin
 * @version $Id: RecapImpl.java$
 */
@Component(label = "Recap Service", metatype = true)
@Service
public class RecapImpl implements Recap {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecapImpl.class);
    private static final BatchReadConfig DEFAULT_BATCH_READ_CONFIG = new RecapBatchReadConfig();

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

    private int defaultPort;
    private String defaultContextPath;
    private String defaultUsername;
    private String defaultPassword;
    private int defaultBatchSize;
    private String defaultLastModifiedProperty;

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
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getDefaultUsername() {
        return defaultUsername;
    }

    public String getDefaultPassword() {
        return defaultPassword;
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
                                    RecapOptions options)
            throws RecapSessionException {

        if (address == null) {
            throw new NullPointerException("address");
        }

        if (StringUtils.isEmpty(address.getHostname())) {
            throw new IllegalArgumentException("address.getHostname() must not be empty");
        }

        RecapAddress addr = applyAddressDefaults(address);
        RecapOptions opts = applyOptionsDefaults(options);
        Session localSession = resourceResolver.adaptTo(Session.class);
        Session srcSession;

        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Repository srcRepo = this.getRepository(addr);
            srcSession = srcRepo.login(
                    new SimpleCredentials(addr.getUsername(),
                            addr.getPassword().toCharArray()));
        } catch (Exception e) {
            throw new RecapSessionException("Failed to login to source repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }

        return new RecapSessionImpl(this, addr, opts, localSession, srcSession);
    }

    private Repository getRepository(RecapAddress recapAddress) throws RepositoryException {
        StringBuilder addressBuilder = new StringBuilder(recapAddress.isHttps() ? "https://" : "http://");
        addressBuilder.append(recapAddress.getHostname());
        if (recapAddress.getPort() != 80) {
            addressBuilder.append(":").append(recapAddress.getPort());
        }
        if (StringUtils.isNotEmpty(recapAddress.getContextPath()) &&
                !"/".equals(recapAddress.getContextPath())) {
            addressBuilder.append(recapAddress.getContextPath());
        }
        addressBuilder.append("/crx/-/jcr:root");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Jcr2spiRepositoryFactory.PARAM_REPOSITORY_SERVICE_FACTORY, Spi2davexRepositoryServiceFactory.class.getName());
        params.put(Jcr2spiRepositoryFactory.PARAM_ITEM_CACHE_SIZE, 128);
        params.put(Jcr2spiRepositoryFactory.PARAM_LOG_WRITER_PROVIDER, new Slf4jLogWriterProvider());
        params.put(Spi2davexRepositoryServiceFactory.PARAM_BATCHREAD_CONFIG, DEFAULT_BATCH_READ_CONFIG);
        params.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, addressBuilder.toString());

        return new RepositoryFactoryImpl().getRepository(params);
    }

    private RecapAddress applyAddressDefaults(final RecapAddress address) {
        RecapAddressImpl dAddress= new RecapAddressImpl();

        dAddress.setPort(defaultPort);
        dAddress.setUsername(defaultUsername);
        dAddress.setPassword(defaultPassword);
        dAddress.setContextPath(defaultContextPath);

        if (address != null) {

            dAddress.setHostname(address.getHostname());
            dAddress.setHttps(address.isHttps());

            if (address.getPort() != null) {
                dAddress.setPort(address.getPort());
            }
            if (address.getUsername() != null) {
                dAddress.setUsername(address.getUsername());
            }
            if (address.getPassword() != null) {
                dAddress.setPassword(address.getPassword());
            }
            if (address.getContextPath() != null) {
                dAddress.setContextPath(address.getContextPath());
            }
        }

        return dAddress;
    }

    private RecapOptions applyOptionsDefaults(final RecapOptions options) {
        RecapOptionsImpl dOptions = new RecapOptionsImpl();
        dOptions.setThrottle(0L);
        dOptions.setBatchSize(defaultBatchSize);
        dOptions.setLastModifiedProperty(defaultLastModifiedProperty);

        if (options != null) {
            dOptions.setUpdate(options.isUpdate());
            dOptions.setOnlyNewer(options.isOnlyNewer());
            if (options.getThrottle() != null) {
                dOptions.setThrottle(options.getThrottle());
            }
            if (options.getBatchSize() != null) {
                dOptions.setBatchSize(options.getBatchSize());
            }
            if (options.getLastModifiedProperty() != null) {
                dOptions.setLastModifiedProperty(options.getLastModifiedProperty());
            }
        }

        return dOptions;
    }
}
