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

package net.adamcin.recap.replication.impl;

import com.day.cq.replication.*;
import com.day.cq.replication.ReplicationLog.Level;
import net.adamcin.recap.api.*;
import net.adamcin.recap.replication.RecapReplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Implements the TransportHandler and PollingTransportHandler interfaces by initializing recap sessions using the
 * AgentConfig connection properties. Specify the transport uri using a recap+http:// or recap+https:// scheme in order
 * to use this transport handler instead of the Http transport handler.
 *
 * IMPORTANT: Leave off the trailing slash on the transport URI to use the default Recap prefix of /crx/server!
 *  A value of "recap+http://localhost:4503/" will likely fail as the slash is interpreted as an explicit prefix path!
 *  Use "recap+http://localhost:4503" instead!
 */
@Component
@Service
public class RecapTransportHandler implements TransportHandler, PollingTransportHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapTransportHandler.class);

    @Reference
    private Recap recap;

    @Reference
    private ResourceResolverFactory resolverFactory;

    /**
     * Builds a RecapAddress from related properties of the provided AgentConfig
     * @param config
     * @return
     * @throws ReplicationException
     */
    public static RecapAddress getAgentAddress(AgentConfig config) throws ReplicationException {

        final String uri = config.getTransportURI();

        if (!uri.startsWith(RecapReplicationConstants.TRANSPORT_URI_SCHEME_PREFIX)) {
            throw new ReplicationException("uri must start with " + RecapReplicationConstants.TRANSPORT_URI_SCHEME_PREFIX);
        }

        final String user = config.getTransportUser();
        final String pass = config.getTransportPassword();

        try {

            final URI parsed = new URI(uri.substring(RecapReplicationConstants.TRANSPORT_URI_SCHEME_PREFIX.length()));
            final boolean https = "https".equals(parsed.getScheme());
            final String host = parsed.getHost();
            final int port = parsed.getPort() < 0 ? (https ? 443 : 80) : parsed.getPort();
            final String prefix = StringUtils.isEmpty(parsed.getPath()) ? null : parsed.getPath();

            return new RecapAddress() {
                public boolean isHttps() { return https; }
                public String getHostname() { return host; }
                public Integer getPort() { return port; }
                public String getUsername() { return user; }
                public String getPassword() { return pass; }
                public String getContextPath() { return null; }
                public String getPrefix() { return prefix; }
            };
        } catch (URISyntaxException e) {
            throw new ReplicationException(e);
        }
    }

    public boolean canHandle(AgentConfig config) {

        String uri = config == null ? null : config.getTransportURI();
        LOGGER.error("[canHandle] uri={}", uri);
        return (uri != null)
                && (uri.startsWith(RecapReplicationConstants.TRANSPORT_URI_SCHEME_PREFIX));
    }

    public ReverseReplication[] poll(TransportContext ctx, ReplicationTransaction tx, ReplicationContentFactory factory)
            throws ReplicationException {
        return new ReverseReplication[0];
    }

    /**
     * Delivers a replication transaction by reverse-syncing the ReplicationAction's list of paths.
     * @param ctx
     * @param tx
     * @return
     * @throws ReplicationException
     */
    public ReplicationResult deliver(TransportContext ctx, ReplicationTransaction tx)
            throws ReplicationException {
        LOGGER.error("[deliver] uri={}", tx.getAction().getPaths());

        if (ctx.getConfig() == null) {
            throw new ReplicationException("config can't be null");
        }

        ReplicationAction action = tx.getAction();

        RecapAddress address = getAgentAddress(ctx.getConfig());

        ResourceResolver resolver = null;

        try {
            resolver = resolverFactory.getAdministrativeResourceResolver(null);

            RecapOptions options = new RecapOptions() {
                // Return true for these options
                public boolean isOnlyNewer() { return true; }
                public boolean isUpdate() { return true; }
                public boolean isReverse() { return true; }

                // TODO: can API calls specify a different last modified property per replication request?
                public String getLastModifiedProperty() { return null; }
                public Integer getBatchSize() { return null; }
                public Long getThrottle() { return null; }
                public BatchReadConfig getBatchReadConfig() { return null; }

                public boolean isNoRecurse() { return false; }
            };

            RecapSession session = recap.initSession(resolver.adaptTo(Session.class), address, options);
            session.setProgressListener(new ReplicationLogProgressListener(tx.getLog()));

            try {
                for (String path : action.getPaths()) {
                    session.sync(path);
                }
            } finally {
                session.finish();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to replicate.", e);
            return new ReplicationResult(false, 1, e.getMessage());
        } finally {
            if (resolver != null) {
                resolver.close();
            }
        }

        return ReplicationResult.OK;
    }

    /**
     * Implementation of RecapProgressListener that wraps a ReplicationLog.
     */
    public static class ReplicationLogProgressListener implements RecapProgressListener {

        private final ReplicationLog log;

        ReplicationLogProgressListener(ReplicationLog log) {
            this.log = log;
        }

        public void onMessage(String fmt, Object... args) {
            if (atLeast(log.getLevel(), Level.INFO)) {
                log.info("M %s", String.format(fmt, args));
            }
        }

        public void onError(String path, Exception ex) {
            if (atLeast(log.getLevel(), Level.WARN)) {
                log.warn("E %s (%s)", path, ex.getMessage());
            }
        }

        public void onFailure(String path, Exception ex) {
            if (atLeast(log.getLevel(), Level.ERROR)) {
                log.warn("F %s (%s)", path, ex.getMessage());
            }
        }

        public void onPath(PathAction action, int count, String path) {
            if (atLeast(log.getLevel(), Level.DEBUG)) {
                log.warn("%s %s", action, path);
            }
        }

        public static boolean atLeast(Level logLevel, Level compareToLevel) {
            return logLevel.compareTo(compareToLevel) <= 0;
        }
    }
}
