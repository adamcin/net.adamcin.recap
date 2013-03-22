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
import net.adamcin.recap.api.*;
import net.adamcin.recap.replication.RecapReplicationUtil;
import net.adamcin.recap.replication.ReplicationLogProgressListener;
import net.adamcin.recap.util.DefaultRecapOptions;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;

/**
 * Implements the TransportHandler interface by initializing recap sessions using the
 * AgentConfig connection properties. Specify the transport uri using a recap+http:// or recap+https:// scheme in order
 * to use this transport handler instead of the Http transport handler.
 *
 * IMPORTANT: Leave off the trailing slash on the transport URI to use the default Recap prefix of /crx/server!
 *  A value of "recap+http://localhost:4503/" will likely fail as the slash is interpreted as an explicit prefix path!
 *  Use "recap+http://localhost:4503" instead!
 */
@Component
@Service
public class DefaultRecapTransportHandler implements TransportHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRecapTransportHandler.class);

    @Reference
    private Recap recap;

    @Reference
    private ResourceResolverFactory resolverFactory;

    public boolean canHandle(AgentConfig config) {

        String uri = config == null ? null : config.getTransportURI();
        LOGGER.error("[canHandle] uri={}", uri);
        return (uri != null) && !config.isSpecific()
                && (uri.startsWith(RecapReplicationUtil.TRANSPORT_URI_SCHEME_PREFIX));
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

        LOGGER.debug("[deliver] uri={}", tx.getAction().getPaths());
        if (ctx.getConfig() == null) {
            throw new ReplicationException("config can't be null");
        }

        ReplicationAction action = tx.getAction();

        RecapAddress address = RecapReplicationUtil.getAgentAddress(ctx.getConfig());

        ResourceResolver resolver = null;

        try {
            resolver = resolverFactory.getAdministrativeResourceResolver(null);

            RecapOptions options = new DefaultRecapOptions() {
                // Return true for these options
                @Override public boolean isOnlyNewer() { return true; }
                @Override public boolean isUpdate() { return true; }
                @Override public boolean isReverse() { return true; }
            };

            RecapSession session = recap.initSession(resolver.adaptTo(Session.class), address, options);
            session.setProgressListener(new ReplicationLogProgressListener(tx.getLog()));

            try {
                if (action.getType() == ReplicationActionType.TEST) {
                    doTest(session, action);
                } else if (action.getType() == ReplicationActionType.ACTIVATE) {
                    doActivateContent(session, action);
                } else if (action.getType() == ReplicationActionType.DEACTIVATE
                        || action.getType() == ReplicationActionType.DELETE) {
                    doDelete(session, action);
                } else {
                    LOGGER.debug("[deliver] replication action type {} not supported.", action.getType());
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

    protected void doTest(RecapSession session, ReplicationAction action)
            throws RecapSessionException {
        for (String path : action.getPaths()) {
            session.checkPermissions(path);
        }
    }

    protected void doActivateContent(RecapSession session, ReplicationAction action)
            throws RecapSessionException {
        for (String path : action.getPaths()) {
            session.syncContent(path);
        }
    }

    protected void doDelete(RecapSession session, ReplicationAction action)
            throws RecapSessionException {
        for (String path : action.getPaths()) {
            session.delete(path);
        }
    }
}
