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
import net.adamcin.recap.util.DefaultRequestDepthConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.client.RepositoryFactoryImpl;
import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.spi.commons.logging.Slf4jLogWriterProvider;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Recap} service implementation, responsible for creating remote {@link Session}s and constructing
 * {@link RecapSession}s. It also serves as the main configuration point for {@link RecapOptions} and
 * {@link RecapAddress} defaults.
 */
@Component(label = "Recap Service", metatype = true)
@Service
public class RecapImpl implements Recap, RecapSessionInterrupter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecapImpl.class);

    @Property(label = "Default Remote Port", intValue = RecapConstants.DEFAULT_DEFAULT_PORT)
    protected static final String OSGI_DEFAULT_PORT = "default.port";

    @Property(label = "Default Remote Username", value = RecapConstants.DEFAULT_DEFAULT_USERNAME)
    protected static final String OSGI_DEFAULT_USERNAME = "default.username";

    @Property(label = "Default Remote Password", value = RecapConstants.DEFAULT_DEFAULT_PASSWORD)
    protected static final String OSGI_DEFAULT_PASSWORD = "default.password";

    @Property(label = "Default Remote DavEx Servlet Path", value = RecapConstants.DEFAULT_DEFAULT_SERVLET_PATH)
    protected static final String OSGI_DEFAULT_SERVLET_PATH = "default.servletPath";

    @Property(label = "Default Batch Size", intValue = RecapConstants.DEFAULT_DEFAULT_BATCH_SIZE)
    protected static final String OSGI_DEFAULT_BATCH_SIZE = "default.batchSize";

    @Property(label = "Default Request Depth Config", value= RecapConstants.DEFAULT_DEFAULT_REQUEST_DEPTH_CONFIG)
    protected static final String OSGI_DEFAULT_REQUEST_DEPTH_CONFIG = "default.requestDepthConfig";

    @Property(label = "Default Last Modified Property", value = RecapConstants.DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY)
    protected static final String OSGI_DEFAULT_LAST_MODIFIED_PROPERTY = "default.lastModifiedProperty";

    private int defaultPort;
    private String defaultServletPath;
    private String defaultUsername;
    private String defaultPassword;
    private int defaultBatchSize;
    private String defaultRequestDepthConfig;
    private String defaultLastModifiedProperty;

    boolean sessionsInterrupted = false;

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> props) {
        this.sessionsInterrupted = false;

        LOGGER.debug("[activate] props={}", props);
        defaultPort = OsgiUtil.toInteger(props.get(OSGI_DEFAULT_PORT), RecapConstants.DEFAULT_DEFAULT_PORT);
        defaultServletPath = OsgiUtil.toString(props.get(OSGI_DEFAULT_SERVLET_PATH), RecapConstants.DEFAULT_DEFAULT_SERVLET_PATH);
        defaultUsername = OsgiUtil.toString(props.get(OSGI_DEFAULT_USERNAME), RecapConstants.DEFAULT_DEFAULT_USERNAME);
        defaultPassword = OsgiUtil.toString(props.get(OSGI_DEFAULT_PASSWORD), RecapConstants.DEFAULT_DEFAULT_PASSWORD);
        defaultBatchSize = OsgiUtil.toInteger(props.get(OSGI_DEFAULT_BATCH_SIZE), RecapConstants.DEFAULT_DEFAULT_BATCH_SIZE);
        defaultRequestDepthConfig = OsgiUtil.toString(props.get(OSGI_DEFAULT_REQUEST_DEPTH_CONFIG), RecapConstants.DEFAULT_DEFAULT_REQUEST_DEPTH_CONFIG);
        defaultLastModifiedProperty = OsgiUtil.toString(props.get(OSGI_DEFAULT_LAST_MODIFIED_PROPERTY), RecapConstants.DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        this.sessionsInterrupted = true;
        defaultPort = 0;
        defaultServletPath = null;
        defaultUsername = null;
        defaultPassword = null;
        defaultBatchSize = 0;
        defaultRequestDepthConfig = null;
        defaultLastModifiedProperty = null;
    }

    public boolean isInterrupted() {
        return this.sessionsInterrupted;
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

    public String getDefaultRequestDepthConfig() {
        return defaultRequestDepthConfig;
    }

    public String getDefaultLastModifiedProperty() {
        return defaultLastModifiedProperty;
    }

    public String getDefaultContextPath() {
        return null;
    }

    public String getDefaultPrefix() {
        return null;
    }

    public String getDefaultServletPath() {
        return defaultServletPath;
    }

    public RecapSession initSession(Session localJcrSession,
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
        LOGGER.debug("[initSession] opts={}", opts);
        Session srcSession;

        ClassLoader orig = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Repository srcRepo = this.getRepository(addr, new BatchReadConfigAdapter(opts.getRequestDepthConfig()));
            srcSession = srcRepo.login(
                    new SimpleCredentials(addr.getUsername(),
                            addr.getPassword().toCharArray()));
        } catch (Exception e) {
            throw new RecapSessionException("Failed to login to source repository.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(orig);
        }

        return new RecapSessionImpl(this, addr, opts, localJcrSession, srcSession);
    }

    private Repository getRepository(RecapAddress recapAddress, BatchReadConfig batchReadConfig) throws RepositoryException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, getRepositoryUrl(recapAddress));
        if (batchReadConfig != null) {
            params.put(Spi2davexRepositoryServiceFactory.PARAM_BATCHREAD_CONFIG, batchReadConfig);
        }
        params.put(Jcr2spiRepositoryFactory.PARAM_REPOSITORY_SERVICE_FACTORY, Spi2davexRepositoryServiceFactory.class.getName());
        params.put(Jcr2spiRepositoryFactory.PARAM_ITEM_CACHE_SIZE, 128);
        params.put(Jcr2spiRepositoryFactory.PARAM_LOG_WRITER_PROVIDER, new Slf4jLogWriterProvider());

        LOGGER.debug("[getRepository] repository SPI params: {}", params);
        return new RepositoryFactoryImpl().getRepository(params);
    }

    private RecapAddress applyAddressDefaults(final RecapAddress address) {
        RecapAddressImpl dAddress= new RecapAddressImpl();

        dAddress.setPort(defaultPort);
        dAddress.setUsername(defaultUsername);
        dAddress.setPassword(defaultPassword);
        dAddress.setServletPath(defaultServletPath);

        if (address != null) {

            dAddress.setHostname(address.getHostname());
            dAddress.setHttps(address.isHttps());

            if (address.getPort() != null && address.getPort() > 0) {
                dAddress.setPort(address.getPort());
            }
            if (address.getUsername() != null) {
                dAddress.setUsername(address.getUsername());
            }
            if (address.getPassword() != null) {
                dAddress.setPassword(address.getPassword());
            }
            if (address.getServletPath() != null) {
                dAddress.setServletPath(address.getServletPath());
            }
        }

        return dAddress;
    }

    private RecapOptions applyOptionsDefaults(final RecapOptions options) {
        RecapOptionsImpl dOptions = new RecapOptionsImpl();
        dOptions.setThrottle(0L);
        dOptions.setBatchSize(defaultBatchSize);
        dOptions.setLastModifiedProperty(defaultLastModifiedProperty);
        dOptions.setRequestDepthConfig(DefaultRequestDepthConfig.parseParameterValue(defaultRequestDepthConfig));

        if (options != null) {
            dOptions.setUpdate(options.isUpdate());
            dOptions.setOnlyNewer(options.isOnlyNewer());
            dOptions.setReverse(options.isReverse());
            dOptions.setNoRecurse(options.isNoRecurse());
            dOptions.setNoDelete(options.isNoDelete());
            dOptions.setKeepOrder(options.isKeepOrder());
            if (options.getThrottle() != null) {
                dOptions.setThrottle(options.getThrottle());
            }
            if (options.getBatchSize() != null) {
                dOptions.setBatchSize(options.getBatchSize());
            }
            if (options.getLastModifiedProperty() != null) {
                dOptions.setLastModifiedProperty(options.getLastModifiedProperty());
            }
            if (options.getRequestDepthConfig() != null) {
                dOptions.setRequestDepthConfig(options.getRequestDepthConfig());
            }
            if (options.getFilter() != null) {
                dOptions.setFilter(options.getFilter());
            }
        }

        return dOptions;
    }

    public String getDisplayableUrl(RecapAddress address) {
        RecapAddress recapAddress = applyAddressDefaults(address);
        StringBuilder addressBuilder = new StringBuilder();
        if (recapAddress.getHostname() != null) {
            addressBuilder.append((recapAddress.isHttps() ? "https://" : "http://"));
            addressBuilder.append(recapAddress.getHostname());
            if (recapAddress.getPort() != null &&
                    !(!recapAddress.isHttps() && recapAddress.getPort() == 80) &&
                    !(recapAddress.isHttps() && recapAddress.getPort() == 443)) {
                addressBuilder.append(":").append(recapAddress.getPort());
            }
            if (StringUtils.isNotEmpty(recapAddress.getServletPath())) {
                addressBuilder.append(recapAddress.getServletPath());
            } else {
                addressBuilder.append("/");
            }
        }
        return addressBuilder.toString();
    }

    public String getRepositoryUrl(RecapAddress address) {
        RecapAddress recapAddress = applyAddressDefaults(address);
        String base = getDisplayableUrl(recapAddress);
        if (StringUtils.isNotEmpty(base)) {
            return base;
        } else {
            return null;
        }
    }
}
