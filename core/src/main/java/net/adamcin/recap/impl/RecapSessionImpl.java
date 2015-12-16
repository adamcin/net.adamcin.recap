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

import net.adamcin.commons.jcr.batch.*;
import net.adamcin.recap.api.*;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import net.adamcin.recap.util.OrderableNodesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import java.util.*;

/**
 * Implementation of {@link RecapSession} using a
 * {@link net.adamcin.commons.jcr.batch.BatchSession} to manage auto-saves
 */
public final class RecapSessionImpl implements RecapSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecapSessionImpl.class);

    private static final int DEFAULT_BATCH_SIZE = 1024;
    private static final long DEFAULT_THROTTLE = 0L;
    private static final RecapFilter DEFAULT_FILTER = new RecapFilter() {
        public boolean includesPath(String path) {
            return true;
        }
    };

    private final RecapSessionInterrupter interrupter;
    private final RecapAddress address;
    private final RecapOptions options;
    private final Session localSession;
    private final Session remoteSession;
    private RecapProgressListener progressListener;

    private final BatchSession targetSession;

    private String lastSuccessfulPath;
    private int totalSyncPaths = 0;
    private int totalNodes = 0;
    private long totalSize = 0L;
    private long currentSize = 0L;
    private long start = 0L;
    private long end = 0L;

    private Map<String, String> prefixMapping = new HashMap<String, String>();
    private boolean allowLastModifiedProperty = false;
    private boolean interrupted = false;
    private boolean finished = false;


    public RecapSessionImpl(RecapSessionInterrupter interrupter,
                            RecapAddress address,
                            RecapOptions options,
                            Session localSession,
                            Session remoteSession) {

        if (interrupter == null) {
            throw new NullPointerException("interrupter");
        }
        if (address == null) {
            throw new NullPointerException("address");
        }
        if (options == null) {
            throw new NullPointerException("options");
        }
        if (localSession == null) {
            throw new NullPointerException("localSession");
        }
        if (remoteSession == null) {
            throw new NullPointerException("remoteSession");
        }
        if (localSession.getRepository().equals(remoteSession.getRepository())
                && localSession.getWorkspace().equals(remoteSession.getWorkspace())) {
            throw new IllegalArgumentException("localSession and remoteSession must not be on the same repository and workspace");
        }

        this.interrupter = interrupter;
        this.address = address;
        this.options = new OptionsShield(options);
        this.localSession = localSession;
        this.remoteSession = remoteSession;

        Session dstSession = getDestinationSession();
        targetSession = new DefaultBatchSession(dstSession);
        targetSession.addListener(new SyncSaveListener());

        allowLastModifiedProperty = isValidNameForSession(this.options.getLastModifiedProperty());
    }

    private Session getSourceSession() {
        if (this.options.isReverse()) {
            return this.localSession;
        } else {
            return this.remoteSession;
        }
    }

    private Session getDestinationSession() {
        if (this.options.isReverse()) {
            return this.remoteSession;
        } else {
            return this.localSession;
        }
    }

    public BatchSession getTargetSession() {
        return targetSession;
    }

    private void start() {
        if (this.start == 0L) {
            this.start = System.currentTimeMillis();
        }
    }

    class SyncSaveListener extends DefaultBatchSessionListener {

        @Override
        public void onSave(BatchSaveInfo info) {
            super.onSave(info);
            long afterSave = System.currentTimeMillis();
            trackMessage("Saved %d nodes (%d kB) in %d ms.",
                    info.getCount(), currentSize / 1000L, info.getTime());
            trackMessage("Total time: %d ms, total nodes %d, %d kB",
                    afterSave - start, totalNodes, totalSize / 1024L);
            currentSize = 0L;
            if (options.getThrottle() > 0L && !interrupted && !finished) {
                trackMessage("Throttling enabled. Waiting %ds...", options.getThrottle());
                try {
                    Thread.sleep(options.getThrottle() * 1000L);
                } catch (InterruptedException e) {
                    LOGGER.debug("[onSave] thread interrupted.");
                }
            }
        }

        @Override
        public void onRemove(BatchRemoveInfo info) {
            super.onRemove(info);
            totalNodes++;
            trackPath(RecapProgressListener.PathAction.DELETE, info.getPath());
        }
    }

    public void checkPermissions(String path) throws RecapSessionException {
        String parentPath = Text.getRelativeParent(path, 1);

        try {
            getSourceSession().checkPermission(path, Session.ACTION_READ);
        } catch (RepositoryException e) {
            trackError(path, e);
        }

        try {
            getTargetSession().checkPermission(parentPath, Session.ACTION_READ);
            getTargetSession().checkPermission(parentPath, Session.ACTION_ADD_NODE);
        } catch (RepositoryException e) {
            trackError(parentPath, e);
        }

        try {
            getTargetSession().checkPermission(path, Session.ACTION_READ);
            getTargetSession().checkPermission(path, Session.ACTION_ADD_NODE);
            getTargetSession().checkPermission(path, Session.ACTION_SET_PROPERTY);
            if (!options.isNoDelete()) {
                getTargetSession().checkPermission(path, Session.ACTION_REMOVE);
            }
        } catch (RepositoryException e) {
            trackError(path, e);
        }
    }

    public Node getOrCreateTargetNode(Node sourceNode) throws RepositoryException {
        if (sourceNode.getDepth() == 0) {
            return getTargetSession().getRootNode();
        } else if (getTargetSession().getRootNode().hasNode(sourceNode.getPath().substring(1))) {
            return getTargetSession().getRootNode().getNode(sourceNode.getPath().substring(1));
        } else {
            Node sourceParent = sourceNode.getParent();
            Node targetParent = getOrCreateTargetNode(sourceParent);
            Node targetNode;
            if (!targetParent.hasNode(sourceNode.getName())) {
                targetNode = targetParent.addNode(sourceNode.getName(), sourceNode.getPrimaryNodeType().getName());
                trackPath(RecapProgressListener.PathAction.ADD, targetNode.getPath());
            } else {
                targetNode = targetParent.getNode(sourceNode.getName());
            }
            return targetNode;
        }
    }

    public void sync(String path) throws RecapSessionException {
        if (this.finished) {
            throw new RecapSessionException("RecapSession already finished.");
        }

        trackMessage("Sync %s %s http://%s:%d/", path, this.options.isReverse() ? "to" : "from", this.address.getHostname(), this.address.getPort());

        try {
            start();

            Node srcNode = getSourceSession().getNode(path);
            Node srcParent = srcNode.getParent();
            Node dstParent = getOrCreateTargetNode(srcParent);

            String dstName = srcNode.getName();

            this.copy(srcNode, dstParent, dstName, !this.options.isNoRecurse());
            this.lastSuccessfulPath = path;
            this.totalSyncPaths++;
        } catch (PathNotFoundException e) {
            LOGGER.debug("PathNotFoundException while preparing path: {}. Message: {}", path, e.getMessage());
            trackError(path, e);
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while copying path: {}. Message: {}", path, e.getMessage());
            trackFailure(path, e);
            this.interrupted = true;
            this.finish();
            throw new RecapSessionException("RepositoryException while preparing path: " + path, e);
        }
    }

    public RecapAddress getAddress() {
        return address;
    }

    public RecapOptions getOptions() {
        return options;
    }

    public boolean isFinished() {
        return finished;
    }

    public void logout() {
        if (this.remoteSession != null) {
            this.remoteSession.logout();
        }
    }

    public RecapProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(RecapProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void trackPath(RecapProgressListener.PathAction action, String path) {
        if (this.getProgressListener() != null) {
            this.getProgressListener().onPath(action, this.totalNodes, path);
        }
    }

    private void trackError(String path, Exception exception) {
        if (this.getProgressListener() != null) {
            this.getProgressListener().onError(path, exception);
        }
    }

    private void trackFailure(String path, Exception exception) {
        if (this.getProgressListener() != null) {
            this.getProgressListener().onFailure(path, exception);
        }
    }

    private void trackMessage(String fmt, Object... args) {
        if (this.getProgressListener() != null) {
            this.getProgressListener().onMessage(fmt, args);
        }
    }

    private void sanityCheck() throws RecapSessionException {
        if (this.finished) {
            throw new RecapSessionException("RecapSession already finished.");
        }
    }

    public void syncContent(String path) throws RecapSessionException {
        sanityCheck();

        trackMessage("Sync %s %s http://%s:%d/", path, this.options.isReverse() ? "to" : "from", this.address.getHostname(), this.address.getPort());

        try {
            if (this.start == 0L) {
                this.start = System.currentTimeMillis();
            }

            Node srcNode = getSourceSession().getNode(path);

            Node srcParent = srcNode.getParent();
            Node dstParent = getOrCreateTargetNode(srcParent);

            String dstName = srcNode.getName();

            this.copy(srcNode, dstParent, dstName, false);

            if (srcNode.hasNode(JcrConstants.JCR_CONTENT)) {
                Node srcContentNode = srcNode.getNode(JcrConstants.JCR_CONTENT);

                this.copy(srcContentNode, dstParent.getNode(dstName), JcrConstants.JCR_CONTENT,
                        !this.options.isNoRecurse());
            }

            this.lastSuccessfulPath = path;
            this.totalSyncPaths++;

        } catch (PathNotFoundException e) {
            LOGGER.debug("PathNotFoundException while preparing path: {}. Message: {}", path, e.getMessage());
            trackError(path, e);
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while copying path: {}. Message: {}", path, e.getMessage());
            trackFailure(path, e);
            this.interrupted = true;
            this.finish();
            throw new RecapSessionException("RepositoryException while preparing path: " + path, e);
        }
    }

    public void delete(String path) throws RecapSessionException {
        sanityCheck();

        if (this.options.isReverse()) {
            trackMessage("Deleting %s from http://%s:%d/", path,
                    this.address.getHostname(), this.address.getPort());
        } else {
            trackMessage("Deleting %s from local repository", path);
        }

        try {
            getTargetSession().removeItem(path);
        } catch (PathNotFoundException e) {
            LOGGER.debug("PathNotFoundException while removing path: {}. Message: {}", path, e.getMessage());
            trackError(path, e);
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while removing path: {}. Message: {}", path, e.getMessage());
            trackFailure(path, e);
            this.interrupted = true;
            this.finish();
            throw new RecapSessionException("RepositoryException while removing path: " + path, e);
        }
    }

    public void finish() throws RecapSessionException {
        if (!this.finished) {
            this.finished = true;
            RecapSessionException exception = null;
            if (!this.interrupted) {
                try {
                    getTargetSession().commit();
                    trackMessage("Done.");
                } catch (RepositoryException e) {
                    LOGGER.error("[finish] Failed to save remaining changes.", e);
                    trackMessage("Failed to save remaining changes. %s", e.getMessage());
                    this.interrupted = true;
                    exception = new RecapSessionException("Failed to save remaining changes.", e);
                }
            }

            this.end = System.currentTimeMillis();
            this.logout();

            if (this.getTotalNodes() > 0) {

                trackMessage("Copy %s. %d nodes in %dms. %d bytes",
                        (this.interrupted ? "interrupted" : "completed"),
                        this.getTotalNodes(), this.getTotalTimeMillis(), this.getTotalSize());

                trackMessage("%d root paths added or updated successfully. Last successful path: %s",
                        this.getTotalSyncPaths(), this.getLastSuccessfulSyncPath());
            }

            if (exception != null) {
                throw exception;
            }
        }
    }

    /**
     * The recursive copy function
     * @param src existing source node
     * @param dstParent existing destination parent node
     * @param dstName name of destination node
     * @param recursive
     * @throws RecapSessionException
     * @throws RepositoryException
     */
    private void copy(Node src, Node dstParent, String dstName, boolean recursive)
            throws RecapSessionException, RepositoryException {

        if (interrupter.isInterrupted()) {
            throw new RecapSessionException("RecapSession interrupted.");
        }

        String path = src.getPath();
        String dstPath = dstParent.getPath() + "/" + dstName;

        boolean useSysView = src.getDefinition().isProtected();
        boolean isNew = false;
        boolean overwrite = this.options.isUpdate();
        boolean included = this.options.getFilter().includesPath(path);

        getTargetSession().disableAutoSave();

        ++totalNodes;
        Node dst;
        if (dstParent.hasNode(dstName)) {
            dst = dstParent.getNode(dstName);
            if (!included) {
                trackPath(RecapProgressListener.PathAction.IGNORE, dstPath);
            } else if (overwrite) {
                if ((this.options.isOnlyNewer()) && (dstName.equals(JcrConstants.JCR_CONTENT))) {
                    if (isNewer(src, dst)) {
                        trackPath(RecapProgressListener.PathAction.UPDATE, dstPath);
                    } else {
                        overwrite = false;
                        recursive = false;
                        trackPath(RecapProgressListener.PathAction.NO_ACTION, dstPath);
                    }
                } else {
                    trackPath(RecapProgressListener.PathAction.UPDATE, dstPath);
                }

                if (useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                }
            } else {
                trackPath(RecapProgressListener.PathAction.NO_ACTION, dstPath);
            }
        } else {
            try {
                if (included && useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                } else {
                    dst = dstParent.addNode(dstName, src.getPrimaryNodeType().getName());
                }
                trackPath(RecapProgressListener.PathAction.ADD, dstPath);
                isNew = true;
            } catch (RepositoryException e) {
                LOGGER.warn("Error while adding node {} (ignored): {}", dstPath, e.toString());
                trackError(dstPath, e);
                return;
            }
        }

        if (included && useSysView) {
            trackTree(dst, isNew);
        } else {
            Set<String> names = new HashSet<String>();
            if (included && ((overwrite) || (isNew))) {
                if (!isNew) {
                    for (NodeType nt : dst.getMixinNodeTypes()) {
                        names.add(nt.getName());
                    }

                    for (NodeType nt : src.getMixinNodeTypes()) {
                        String mixName = checkNameSpace(nt.getName());
                        if (!names.remove(mixName)) {
                            dst.addMixin(nt.getName());
                        }
                    }

                    for (String mix : names) {
                        dst.removeMixin(mix);
                    }
                } else {
                    for (NodeType nt : src.getMixinNodeTypes()) {
                        dst.addMixin(checkNameSpace(nt.getName()));
                    }
                }

                names.clear();
                if (!isNew) {
                    PropertyIterator iter = dst.getProperties();
                    while (iter.hasNext()) {
                        names.add(checkNameSpace(iter.nextProperty().getName()));
                    }
                }
                PropertyIterator iter = src.getProperties();
                while (iter.hasNext()) {
                    Property p = iter.nextProperty();
                    String pName = checkNameSpace(p.getName());
                    names.remove(pName);

                    if (p.getDefinition().isProtected()) {
                        continue;
                    }
                    if (dst.hasProperty(pName)) {
                        dst.getProperty(pName).remove();
                    }
                    if (p.getDefinition().isMultiple()) {
                        Value[] vs = p.getValues();
                        dst.setProperty(pName, vs);
                        for (long s : p.getLengths()) {
                            this.totalSize += s;
                            this.currentSize += s;
                        }
                    } else {
                        Value v = p.getValue();
                        dst.setProperty(pName, v);
                        long s = p.getLength();
                        this.totalSize += s;
                        this.currentSize += s;
                    }
                }

                for (String pName : names) {
                    try {
                        dst.getProperty(pName).remove();
                    } catch (RepositoryException e) {
                        LOGGER.warn("[copy] failed to remove property {} from node {}", pName, path);
                    }
                }
            }

            // re-enable auto-save before recursion
            getTargetSession().enableAutoSave();

            if (recursive) {
                names.clear();
                if ((overwrite) && (!isNew)) {
                    NodeIterator niter = dst.getNodes();
                    while (niter.hasNext()) {
                        names.add(checkNameSpace(niter.nextNode().getName()));
                    }
                }
                NodeIterator niter = src.getNodes();
                while (niter.hasNext()) {
                    Node child = niter.nextNode();
                    String cName = checkNameSpace(child.getName());
                    names.remove(cName);
                    copy(child, dst, cName, true);
                }

                if (!options.isNoDelete()) {
                    for (String name : names) {
                        try {
                            Node cNode = dst.getNode(name);
                            cNode.remove();
                        } catch (RepositoryException e) {
                            LOGGER.warn("[copy] failed to delete existing node in dst that does not exist in src", e);
                        }
                    }
                }

                if (options.isKeepOrder() && !isNew && supportsOrdering(src) && supportsOrdering(dst)) {
                    OrderableNodesList srcChildren = new OrderableNodesList(src);
                    OrderableNodesList dstChildren = new OrderableNodesList(dst);
                    while(!ensureOrder(srcChildren, dstChildren)) {
                        //reorder children
                    }
                }
            }
        }
    }

    private boolean supportsOrdering(Node node) throws RepositoryException {
        return node.getPrimaryNodeType().hasOrderableChildNodes();
    }

    private boolean ensureOrder(OrderableNodesList srcChildren, OrderableNodesList dstChildren) throws RepositoryException {
        Iterator<String> srcIter = srcChildren.iterator();
        Iterator<String> dstIter = dstChildren.iterator();
        while (srcIter.hasNext() && dstIter.hasNext()) {
            String srcNodeName = srcIter.next();
            String dstNodeName = dstIter.next();

            if (!srcNodeName.equals(dstNodeName) && dstChildren.contains(srcNodeName)) {
                int dstNodePos = srcChildren.getPos(dstNodeName);
                dstChildren.moveExistingNode(dstNodeName, dstNodePos);
                trackPath(RecapProgressListener.PathAction.MOVE, dstChildren.getRepositoryPath(dstNodeName));
                return false;
            }
        }

        return true;
    }

    private Node sysCopy(Node src, Node dstParent, String dstName)
            throws RepositoryException {
        try {
            ContentHandler handler = dstParent.getSession().getImportContentHandler(dstParent.getPath(), 0);
            src.getSession().exportSystemView(src.getPath(), handler, true, false);
            return dstParent.getNode(dstName);
        } catch (SAXException e) {
            throw new RepositoryException("Unable to perform sysview copy", e);
        }
    }

    private void trackTree(Node node, boolean isNew) throws RepositoryException {
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            if (isNew) {
                trackPath(RecapProgressListener.PathAction.ADD, child.getPath());
            } else {
                trackPath(RecapProgressListener.PathAction.UPDATE, child.getPath());
            }
            trackTree(child, isNew);
        }
    }

    private boolean isNewer(Node src, Node dst) {
        try {
            Calendar srcDate = null;
            Calendar dstDate = null;
            if ((this.allowLastModifiedProperty) && (src.hasProperty(this.options.getLastModifiedProperty())) && (dst.hasProperty(this.options.getLastModifiedProperty()))) {
                srcDate = src.getProperty(this.options.getLastModifiedProperty()).getDate();
                dstDate = dst.getProperty(this.options.getLastModifiedProperty()).getDate();
            } else if ((src.hasProperty(JcrConstants.JCR_LASTMODIFIED)) && (dst.hasProperty(JcrConstants.JCR_LASTMODIFIED))) {
                srcDate = src.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
                dstDate = dst.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
            }
            return (srcDate == null) || (dstDate == null) || (srcDate.after(dstDate));
        } catch (RepositoryException e) {
            LOGGER.error("Unable to compare dates: {}", e.toString());
        }
        return true;
    }

    private boolean isValidNameForSession(String name) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }

        try {
            mapPrefixedName(name);
            return true;
        } catch (RepositoryException e) {
            LOGGER.error("Error processing namespace for {}: {}", name, e.toString());
            return false;
        }
    }

    private String mapPrefixedName(String name) throws RepositoryException {
        int idx = name.indexOf(':');
        if (idx > 0) {
            String prefix = name.substring(0, idx);
            String mapped = this.prefixMapping.get(prefix);
            if (mapped == null) {
                String uri = getSourceSession().getNamespaceURI(prefix);
                int i = -1;
                try {
                    mapped = getTargetSession().getNamespacePrefix(uri);
                } catch (NamespaceException e) {
                    mapped = prefix;
                    i = 0;
                }

                while (i >= 0) {
                    try {
                        getTargetSession().getWorkspace().getNamespaceRegistry().registerNamespace(mapped, uri);
                        i = -1;
                    } catch (NamespaceException e1) {
                        mapped = prefix + i++;
                    }
                }

                this.prefixMapping.put(prefix, mapped);
            }
            if (mapped.equals(prefix)) {
                return name;
            }
            return mapped + prefix.substring(idx);
        }

        return name;
    }

    private String checkNameSpace(String name) {
        try {
            name = mapPrefixedName(name);
        } catch (RepositoryException e) {
            LOGGER.error("Error processing namespace for {}: {}", name, e.toString());
        }
        return name;
    }

    public Session getRemoteSession() {
        return this.remoteSession;
    }

    public Session getLocalSession() {
        return this.localSession;
    }

    public int getTotalSyncPaths() {
        return this.totalSyncPaths;
    }

    public String getLastSuccessfulSyncPath() {
        return this.lastSuccessfulPath;
    }

    public int getTotalNodes() {
        return this.totalNodes;
    }

    public long getTotalSize() {
        return this.totalSize;
    }

    public long getTotalTimeMillis() {
        return this.end - this.start;
    }

    static class OptionsShield implements RecapOptions {
        final RecapOptions unsafe;

        OptionsShield(RecapOptions unsafe) {
            if (unsafe == null) {
                throw new NullPointerException("unsafe");
            }
            this.unsafe = unsafe;
        }

        public Integer getBatchSize() {
            Integer unsafeBatchSize = unsafe.getBatchSize();
            if (unsafeBatchSize == null) {
                LOGGER.debug("[OptionsShield#getBatchSize] null value for batchSize; using hard coded default of {}",
                        DEFAULT_BATCH_SIZE);
                return DEFAULT_BATCH_SIZE;
            } else {
                return unsafeBatchSize;
            }
        }

        public Long getThrottle() {
            Long unsafeThrottle = unsafe.getThrottle();
            if (unsafeThrottle == null) {
                LOGGER.debug("[OptionsShield#getBatchSize] null value for throttle; using hard coded default of {}",
                        DEFAULT_THROTTLE);
                return DEFAULT_THROTTLE;
            } else {
                return unsafeThrottle;
            }
        }

        public RecapFilter getFilter() {
            RecapFilter unsafeFilter = unsafe.getFilter();
            if (unsafeFilter == null) {
                LOGGER.debug("[OptionsShield#getFilter] null value for filter; using hard coded default filter");
                return DEFAULT_FILTER;
            } else {
                return unsafeFilter;
            }
        }

        public String getLastModifiedProperty() { return unsafe.getLastModifiedProperty(); }
        public RequestDepthConfig getRequestDepthConfig() { return unsafe.getRequestDepthConfig(); }
        public boolean isOnlyNewer() { return unsafe.isOnlyNewer(); }
        public boolean isUpdate() { return unsafe.isUpdate(); }
        public boolean isReverse() { return unsafe.isReverse(); }
        public boolean isNoRecurse() { return unsafe.isNoRecurse(); }
        public boolean isNoDelete() { return unsafe.isNoDelete(); }
        public boolean isKeepOrder() { return unsafe.isKeepOrder(); }
        @Override public String toString() { return unsafe.toString(); }
    }
}
