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

package net.adamcin.commons.jcr.batch;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.*;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.retention.RetentionManager;
import javax.jcr.security.AccessControlManager;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.AccessControlException;
import java.util.*;

/**
 * Default implementation of the {@link BatchSession} interface. This is quite a massive class due to the complexity of
 * the JCR {@link Session}, {@link Node}, and {@link Property} interfaces
 */
public final class DefaultBatchSession implements BatchSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBatchSession.class);

    private final Session session;
    private boolean sessionSaveEnabled = true;
    private boolean autoSaveEnabled = true;

    private int batchSize = 1024;
    private int totalSaves = 0;

    private final VersionManager versionManager;
    private final List<Version> versionsToPurge = new LinkedList<Version>();
    private final List<BatchSessionListener> listeners = new ArrayList<BatchSessionListener>();

    private final Set<String> uncommittedPaths = new HashSet<String>();
    private final Set<String> committedPaths = new HashSet<String>();

    public DefaultBatchSession(Session session) {
        this.session = session;

        VersionManager _vm = null;
        try {
            _vm = session.getWorkspace().getVersionManager();
        } catch (RepositoryException e) {
            LOGGER.error("[BatchManagerImpl] Failed to get VersionManager. Will not purge version history for deleted nodes");
        }

        this.versionManager = _vm;
    }

    //------------------------------------------------------------
    // BatchSession methods
    //------------------------------------------------------------
    public boolean disableSessionSave() {
        if (sessionSaveEnabled) {
            sessionSaveEnabled = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean enableSessionSave() {
        if (!sessionSaveEnabled) {
            sessionSaveEnabled = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean disableAutoSave() {
        if (autoSaveEnabled) {
            autoSaveEnabled = false;
            return true;
        } else {
            return false;
        }
    }

    public boolean enableAutoSave() {
        if (!autoSaveEnabled) {
            autoSaveEnabled = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean isSessionSaveEnabled() {
        return sessionSaveEnabled;
    }

    public boolean isAutoSaveEnabled() {
        return autoSaveEnabled;
    }

    /**
     * The client should record all totalPaths modified during JCR operations and call this method with
     * those totalPaths so that the BatchManagerImpl can track the number of changes in the current batch.
     * If the sum of the number of newly modified totalPaths and the number previously uncommitted changes equals or
     * exceeds the batch size, the session will be saved and the listener's onSave() method
     * will be called with the number of changes committed by this save and a set containing the modified totalPaths.
     * If the value of the moreChanges argument is less than 0, the BatchManagerImpl will immediately
     * save any uncommitted changes.
     *
     * @param changedPaths JCR totalPaths that were modified since the last call to processChanges. Must not be null.
     * @throws javax.jcr.RepositoryException if the BatchManagerImpl is not live, or if the call to session.save() fails.
     */
    private void processChanges(String... changedPaths) throws RepositoryException {
        if (changedPaths == null) {
            throw new NullPointerException("changedPaths");
        }
        processChanges(Arrays.asList(changedPaths), false);
    }
    
    /**
     * The client should count all local JCR modifications and call this method with
     * that count so that the BatchManagerImpl can track the number of changes in the current batch.
     * If the sum of the moreChanges argument and previously uncommitted changes equals or
     * exceeds the batch size, the session will be saved and the listener's onSave() method
     * will be called with the number of changes committed by this save.
     * If the value of the moreChanges argument is less than 0, the BatchManagerImpl will immediately
     * save any uncommitted changes.
     *
     * @param changedPaths collection of JCR totalPaths that were modified since the last call to processChanges.
     *                     May be empty. Must not be null.
     * @throws javax.jcr.RepositoryException if the BatchManagerImpl is not live, or if the call to session.save() fails.
     */
    private void processChanges(Collection<String> changedPaths, boolean sessionSave) throws RepositoryException {
        if (changedPaths == null) {
            throw new NullPointerException("changedPaths");
        }

        // check for null or dead session
        if (!this.isLive()) {
            throw new RepositoryException("[processChanges] JCR Session is not live.");
        }

        // merge totalPaths into this.uncommittedPaths
        this.uncommittedPaths.addAll(changedPaths);

        // If moreChanges is negative, or if the sum of moreChanges and this.uncommitted changes is
        // greater than or equal to the batch size, save the session and notify the listener
        if (sessionSave || this.uncommittedPaths.size() >= this.batchSize) {

            final long saveDuration = this.internalSave(sessionSave);
            if (saveDuration >= 0L) {
                this.totalSaves++;

                // Remove any purged Versions
                final int purgedVersionCount = versionManager == null ? 0 : purgeVersions();

                // Remember the current value of this.uncommittedChanges
                final int savedChanges = this.uncommittedPaths.size();

                // Remember the current set of changed totalPaths
                final Set<String> savedPaths = Collections.unmodifiableSet(
                        new HashSet<String>(this.uncommittedPaths));

                // Reset uncommitted batch
                refresh(false);

                // Add the totalPaths to this.totalPaths
                this.committedPaths.addAll(savedPaths);

                // Notify the listener
                BatchSaveInfo info = new BatchSaveInfo() {
                    public int getCount() { return savedChanges; }
                    public Set<String> getPaths() {  return savedPaths; }
                    public long getTime() { return saveDuration; }
                    public int getPurgedVersionCount() { return purgedVersionCount; }
                };

                for (BatchSessionListener listener : this.listeners) {
                    listener.onSave(info);
                }
            }
        }
    }

    public void commit() throws RepositoryException {
        this.enableAutoSave();
        this.enableSessionSave();
        this.save();
    }

    public void save() throws RepositoryException {
        if (sessionSaveEnabled) {
            processChanges(Collections.<String>emptySet(), true);
        }
    }

    public void refresh(boolean preserveChanges) throws RepositoryException {
        if (!preserveChanges) {
            this.reset();
        }
        getSession().refresh(preserveChanges);
    }

    private void reset() {
        this.uncommittedPaths.clear();
    }

    public Session getSession() {
        return this.session;
    }

    public void purge(final String path) throws RepositoryException {
        if (path != null) {
            Node _node = getSession().getRootNode().getNode(path.substring(1));
            _node.accept(new RemoveVisitor(true));
        }
    }

    private long internalSave(boolean sessionSave) throws RepositoryException {
        if (autoSaveEnabled || (sessionSaveEnabled && sessionSave)) {
            long now = System.currentTimeMillis();
            // Save the session
            if (this.session.hasPendingChanges()) {
                this.session.save();
            }

            return System.currentTimeMillis() - now;
        } else {
            return -1L;
        }
    }

    private int purgeVersions() throws RepositoryException {
        int count = 0;
        Iterator<Version> versions = this.versionsToPurge.iterator();
        while (versions.hasNext()) {
            Version version = versions.next();
            if (!JcrConstants.JCR_ROOTVERSION.equals(version.getName())) {
                version.getContainingHistory().removeVersion(version.getName());
                count++;
            }
            versions.remove();
        }
        return count;
    }

    public void addListener(BatchSessionListener listener) {
        if (!this.listeners.contains(listener)) {
            this.listeners.add(listener);
        }
    }

    public void removeListener(BatchSessionListener listener) {
        if (this.listeners.contains(listener)) {
            this.listeners.remove(listener);
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getTotalSaves() {
        return totalSaves;
    }

    public Set<String> getCommittedPaths() {
        return Collections.unmodifiableSet(this.committedPaths);
    }

    public Set<String> getUncommittedPaths() {
        return Collections.unmodifiableSet(this.committedPaths);
    }

    public void move(String fromPath, String toPath) throws RepositoryException {
        final String fromParentPath = Text.getRelativeParent(fromPath, 1);
        final boolean appendName = getSession().nodeExists(toPath);
        final String toParentPath = appendName ? toPath : Text.getRelativeParent(toPath, 1);
        final String finalPath = appendName ? toPath + "/" + Text.getName(fromPath) : toPath;
        getSession().move(fromPath, toPath);
        processChanges(fromParentPath, fromPath, toParentPath, finalPath);
    }

    public void removeItem(String path) throws RepositoryException {
        Item item = getSession().getItem(path);
        if (item.isNode()) {
            new NodeProxy((Node) item).remove();
        } else {
            item.remove();
            processChanges(Text.getRelativeParent(path, 1), path);
        }
    }

    private class RemoveVisitor extends TraversingItemVisitor {
        private Set<String> changes = new HashSet<String>();
        final boolean purgeVersionHistory;
        String rootPath = null;

        RemoveVisitor(final boolean purgeVersionHistory) throws RepositoryException {
            this.purgeVersionHistory = purgeVersionHistory;
        }

        @Override
        protected void entering(Property property, int level)
                throws RepositoryException { /* do nothing */ }

        @Override
        protected void entering(Node node, int level)
                throws RepositoryException {
            final String path = node.getPath();
            if (level == 0) {
                rootPath = path;
            }
            if (versionManager != null && node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                if (!versionManager.isCheckedOut(path)) {
                    versionManager.checkout(path);
                }
            }
        }

        @Override
        protected void leaving(Property property, int level)
                throws RepositoryException { /* do nothing */ }

        /**
         * This is the correct method to implement for recursive deletes
         * to ensure that children are deleted before their parents.
         * @param node the node that will now be removed
         * @param level the level of the node below the root visited node
         * @throws RepositoryException
         */
        @Override
        protected void leaving(final Node node, final int level)
                throws RepositoryException {

            final String path = node.getPath();
            List<Version> versions = null;
            if (purgeVersionHistory) {
                versions = getVersions(node);
                versionsToPurge.addAll(versions);
            }

            final int versionCount = versions == null ? 0 : versions.size();

            // Defer deletion if the node cannot be deleted without deleting the parent
            // as well (which is enforced for mandatory and protected nodes)
            if (node.getDefinition().isMandatory() || node.getDefinition().isProtected()) {
                changes.add(path);
            } else {
                changes.add(node.getParent().getPath());
                changes.add(path);
                try {
                    node.remove();
                    BatchRemoveInfo info = new BatchRemoveInfo() {
                        public String getRootPath() { return rootPath; }
                        public String getPath() { return path; }
                        public int getDepth() { return level; }
                        public int getPurgedVersionCount() { return versionCount; }
                    };

                    for (BatchSessionListener listener : listeners) {
                        listener.onRemove(info);
                    }

                    processChanges(changes.toArray(new String[changes.size()]));
                } finally {
                    changes.clear();
                }
            }
        }

        private List<Version> getVersions(final Node node)
                throws RepositoryException {
            List<Version> versions = new ArrayList<Version>();
            final String path = node.getPath();
            if (versionManager != null && node.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                VersionHistory vh = versionManager.getVersionHistory(path);
                VersionIterator vit = vh.getAllVersions();
                while (vit.hasNext()) {
                    Version version = vit.nextVersion();
                    if (!JcrConstants.JCR_ROOTVERSION.equals(version.getName())) {
                        versions.add(version);
                    }
                }
            }

            return versions;
        }
    }

    //------------------------------------------------------------
    // Commit-first Session methods
    //------------------------------------------------------------
    public ContentHandler getImportContentHandler(String s, int i) throws RepositoryException {
        commit();
        return getSession().getImportContentHandler(s, i);
    }

    public void importXML(String s, InputStream inputStream, int i) throws IOException, RepositoryException {
        commit();
        getSession().importXML(s, inputStream, i);
    }

    public void setNamespacePrefix(String s, String s2) throws RepositoryException {
        commit();
        getSession().setNamespacePrefix(s, s2);
    }

    //------------------------------------------------------------
    // Item Proxy Session methods
    //------------------------------------------------------------

    public Node getRootNode() throws RepositoryException {
        return new NodeProxy(getSession().getRootNode());
    }

    public Node getNodeByUUID(String uuid) throws RepositoryException {
        return new NodeProxy(getSession().getNodeByUUID(uuid));
    }

    public Node getNodeByIdentifier(String identifier) throws RepositoryException {
        return new NodeProxy(getSession().getNodeByIdentifier(identifier));
    }

    public Item getItem(String path) throws RepositoryException {
        Item item = getSession().getItem(path);
        if (item.isNode()) {
            return new NodeProxy((Node) item);
        } else {
            return new PropertyProxy((Property) item);
        }
    }

    public Node getNode(String path) throws RepositoryException {
        return new NodeProxy(getSession().getNode(path));
    }

    public Property getProperty(String path) throws RepositoryException {
        Property property = getSession().getProperty(path);
        return new PropertyProxy(property, new NodeProxy(property.getParent()));
    }

    private abstract class ItemProxy <T extends Item> implements Item {

        protected final T item;
        protected NodeProxy parent;

        protected ItemProxy(T item) {
            this.item = item;
        }

        protected ItemProxy(T item, NodeProxy parent) {
            this.item = item;
            this.parent = parent;
        }

        public Item getAncestor(int i) throws RepositoryException {
            Item ancestor = this.item.getAncestor(i);
            if (ancestor.isNode()) {
                return new NodeProxy((Node) ancestor);
            } else {
                return new PropertyProxy((Property) ancestor, new NodeProxy(ancestor.getParent()));
            }
        }

        public Node getParent() throws RepositoryException {
            if (this.parent != null) {
                this.parent = new NodeProxy(this.item.getParent());
            }
            return this.parent;
        }

        public Session getSession() throws RepositoryException {
            return DefaultBatchSession.this;
        }

        public void accept(ItemVisitor itemVisitor) throws RepositoryException {
            if (this.isNode()) {
                itemVisitor.visit((NodeProxy) this);
            } else {
                itemVisitor.visit((PropertyProxy) this);
            }
        }


        public String getPath() throws RepositoryException
        { return this.item.getPath(); }

        public String getName() throws RepositoryException
        { return this.item.getName(); }

        public int getDepth() throws RepositoryException
        { return this.item.getDepth(); }

        public boolean isNew()
        { return this.item.isNew(); }

        public boolean isModified()
        { return this.item.isModified(); }

        public boolean isSame(Item item) throws RepositoryException
        { return this.item.isSame(item); }

        public void save() throws RepositoryException
        { this.item.save(); }

        public void refresh(boolean b) throws RepositoryException
        { this.item.refresh(b); }
    }

    private final class NodeProxy extends ItemProxy<Node> implements Node {

        NodeProxy(Node item) {
            super(item);
        }

        NodeProxy(Node item, NodeProxy parent) {
            super(item, parent);
        }

        public boolean isNode() { return true; }

        public void remove() throws RepositoryException {
            this.item.accept(new RemoveVisitor(false));
        }

        public Node addNode(String name) throws RepositoryException {
            Node child = this.item.addNode(name);
            processChanges(this.getPath(), child.getPath());
            return new NodeProxy(child);
        }

        public Node addNode(String name, String primaryType) throws RepositoryException {
            Node child = this.item.addNode(name, primaryType);
            processChanges(this.getPath(), child.getPath());
            return new NodeProxy(child);
        }

        public void orderBefore(String s, String s2) throws RepositoryException {
            this.item.orderBefore(s, s2);
            processChanges(this.getPath());
        }

        public Property setProperty(String s, Value value) throws RepositoryException {
            Property property = this.item.setProperty(s, value);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Value value, int i) throws RepositoryException {
            Property property = this.item.setProperty(s, value, i);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Value[] values) throws RepositoryException {
            Property property = this.item.setProperty(s, values);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Value[] values, int i) throws RepositoryException {
            Property property = this.item.setProperty(s, values, i);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, String[] strings) throws RepositoryException {
            Property property = this.item.setProperty(s, strings);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, String[] strings, int i) throws RepositoryException {
            Property property = this.item.setProperty(s, strings, i);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, String s2) throws RepositoryException {
            Property property = this.item.setProperty(s, s2);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, String s2, int i) throws RepositoryException {
            Property property = this.item.setProperty(s, s2, i);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, InputStream inputStream) throws RepositoryException {
            Property property = this.item.setProperty(s, inputStream);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Binary binary) throws RepositoryException {
            Property property = this.item.setProperty(s, binary);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, boolean b) throws RepositoryException {
            Property property = this.item.setProperty(s, b);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, double v) throws RepositoryException {
            Property property = this.item.setProperty(s, v);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, BigDecimal bigDecimal) throws RepositoryException {
            Property property = this.item.setProperty(s, bigDecimal);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, long l) throws RepositoryException {
            Property property = this.item.setProperty(s, l);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Calendar calendar) throws RepositoryException {
            Property property = this.item.setProperty(s, calendar);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Property setProperty(String s, Node node) throws RepositoryException {
            Property property = this.item.setProperty(s, node);
            processChanges(this.getPath());
            return new PropertyProxy(property, this);
        }

        public Node getNode(String s) throws RepositoryException {
            return new NodeProxy(this.item.getNode(s));
        }

        public NodeIterator getNodes() throws RepositoryException {
            return new NodeIteratorProxy(this.item.getNodes(), this);
        }

        public NodeIterator getNodes(String s) throws RepositoryException {
            return new NodeIteratorProxy(this.item.getNodes(s), this);
        }

        public NodeIterator getNodes(String[] strings) throws RepositoryException {
            return new NodeIteratorProxy(this.item.getNodes(strings), this);
        }

        public Property getProperty(String s) throws RepositoryException {
            Property property = this.item.getProperty(s);
            Node parent = property.getParent();
            return new PropertyProxy(property, item == parent ? this : new NodeProxy(parent));
        }

        public PropertyIterator getProperties() throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getProperties(), this);
        }

        public PropertyIterator getProperties(String s) throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getProperties(s), this);
        }

        public PropertyIterator getProperties(String[] strings) throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getProperties(strings), this);
        }

        public Item getPrimaryItem() throws RepositoryException {
            Item primaryItem = this.item.getPrimaryItem();
            if (primaryItem.isNode()) {
                return new NodeProxy((Node) primaryItem, this);
            } else {
                return new PropertyProxy((Property) primaryItem, this);
            }
        }

        public String getUUID() throws RepositoryException {
            return this.item.getUUID();
        }

        public String getIdentifier() throws RepositoryException {
            return this.item.getIdentifier();
        }

        public int getIndex() throws RepositoryException {
            return this.item.getIndex();
        }

        public PropertyIterator getReferences() throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getReferences());
        }

        public PropertyIterator getReferences(String s) throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getReferences(s));
        }

        public PropertyIterator getWeakReferences() throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getWeakReferences());
        }

        public PropertyIterator getWeakReferences(String s) throws RepositoryException {
            return new PropertyIteratorProxy(this.item.getWeakReferences(s));
        }

        public boolean hasNode(String s) throws RepositoryException {
            return this.item.hasNode(s);
        }

        public boolean hasProperty(String s) throws RepositoryException {
            return this.item.hasProperty(s);
        }

        public boolean hasNodes() throws RepositoryException {
            return this.item.hasNodes();
        }

        public boolean hasProperties() throws RepositoryException {
            return this.item.hasProperties();
        }

        public NodeType getPrimaryNodeType() throws RepositoryException {
            return this.item.getPrimaryNodeType();
        }

        public NodeType[] getMixinNodeTypes() throws RepositoryException {
            return this.item.getMixinNodeTypes();
        }

        public boolean isNodeType(String s) throws RepositoryException {
            return this.item.isNodeType(s);
        }

        public void setPrimaryType(String s) throws RepositoryException {
            this.item.setPrimaryType(s);
            processChanges(this.getPath());
        }

        public void addMixin(String s) throws RepositoryException {
            this.item.addMixin(s);
            processChanges(this.getPath());
        }

        public void removeMixin(String s) throws RepositoryException {
            this.item.removeMixin(s);
            processChanges(this.getPath());
        }

        public boolean canAddMixin(String s) throws RepositoryException {
            return this.item.canAddMixin(s);
        }

        public NodeDefinition getDefinition() throws RepositoryException {
            return this.item.getDefinition();
        }

        public Version checkin() throws RepositoryException {
            return this.item.checkin();
        }

        public void checkout() throws RepositoryException {
            this.item.checkout();
        }

        public void doneMerge(Version version) throws RepositoryException {
            this.item.doneMerge(version);
        }

        public void cancelMerge(Version version) throws RepositoryException {
            this.item.cancelMerge(version);
        }

        public void update(String s) throws RepositoryException {
            this.item.update(s);
        }

        public NodeIterator merge(String s, boolean b) throws RepositoryException {
            return new NodeIteratorProxy(this.item.merge(s, b));
        }

        public String getCorrespondingNodePath(String s) throws RepositoryException {
            return this.item.getCorrespondingNodePath(s);
        }

        public NodeIterator getSharedSet() throws RepositoryException {
            return new NodeIteratorProxy(this.item.getSharedSet());
        }

        public void removeSharedSet() throws RepositoryException {
            this.item.removeSharedSet();
        }

        public void removeShare() throws RepositoryException {
            this.item.removeShare();
        }

        public boolean isCheckedOut() throws RepositoryException {
            return this.item.isCheckedOut();
        }

        public void restore(String s, boolean b) throws RepositoryException {
            this.item.restore(s, b);
        }

        public void restore(Version version, boolean b) throws RepositoryException {
            this.item.restore(version, b);
        }

        public void restore(Version version, String s, boolean b) throws RepositoryException {
            this.item.restore(version, s, b);
        }

        public void restoreByLabel(String s, boolean b) throws RepositoryException {
            this.item.restoreByLabel(s, b);
        }

        public VersionHistory getVersionHistory() throws RepositoryException {
            return this.item.getVersionHistory();
        }

        public Version getBaseVersion() throws RepositoryException {
            return this.item.getBaseVersion();
        }

        public Lock lock(boolean b, boolean b2) throws RepositoryException {
            return this.item.lock(b, b2);
        }

        public Lock getLock() throws RepositoryException {
            return this.item.getLock();
        }

        public void unlock() throws RepositoryException {
            this.item.unlock();
        }

        @SuppressWarnings("deprecated")
        public boolean holdsLock() throws RepositoryException {
            return this.item.holdsLock();
        }

        public boolean isLocked() throws RepositoryException {
            return this.item.isLocked();
        }

        public void followLifecycleTransition(String s) throws RepositoryException {
            this.item.followLifecycleTransition(s);
        }

        public String[] getAllowedLifecycleTransistions() throws RepositoryException {
            return this.item.getAllowedLifecycleTransistions();
        }
    }

    private final class PropertyProxy extends ItemProxy<Property> implements Property {

        PropertyProxy(Property item) {
            super(item);
        }

        PropertyProxy(Property item, NodeProxy parent) {
            super(item, parent);
        }

        public boolean isNode() {
            return false;
        }

        public Node getNode() throws RepositoryException {
            return new NodeProxy(this.item.getNode());
        }

        public Property getProperty() throws RepositoryException {
            Property property = this.item.getProperty();
            return new PropertyProxy(property, new NodeProxy(property.getParent()));
        }

        public void remove() throws RepositoryException {
            String parentPath = this.getParent().getPath();
            this.item.remove();
            processChanges(parentPath);
        }

        public void setValue(Value value) throws RepositoryException {
            this.item.setValue(value);
            processChanges(getParent().getPath());
        }

        public void setValue(Value[] values) throws RepositoryException {
            this.item.setValue(values);
            processChanges(getParent().getPath());
        }

        public void setValue(String s) throws RepositoryException {
            this.item.setValue(s);
            processChanges(getParent().getPath());
        }

        public void setValue(String[] strings) throws RepositoryException {
            this.item.setValue(strings);
            processChanges(getParent().getPath());
        }

        public void setValue(InputStream inputStream) throws RepositoryException {
            this.item.setValue(inputStream);
            processChanges(getParent().getPath());
        }

        public void setValue(Binary binary) throws RepositoryException {
            this.item.setValue(binary);
            processChanges(getParent().getPath());
        }

        public void setValue(long l) throws RepositoryException {
            this.item.setValue(l);
            processChanges(getParent().getPath());
        }

        public void setValue(double v) throws RepositoryException {
            this.item.setValue(v);
            processChanges(getParent().getPath());
        }

        public void setValue(BigDecimal bigDecimal) throws RepositoryException {
            this.item.setValue(bigDecimal);
            processChanges(getParent().getPath());
        }

        public void setValue(Calendar calendar) throws RepositoryException {
            this.item.setValue(calendar);
            processChanges(getParent().getPath());
        }

        public void setValue(boolean b) throws RepositoryException {
            this.item.setValue(b);
            processChanges(getParent().getPath());
        }

        public void setValue(Node node) throws RepositoryException {
            this.item.setValue(node);
            processChanges(getParent().getPath());
        }

        public Value getValue() throws RepositoryException
        { return this.item.getValue(); }

        public Value[] getValues() throws RepositoryException
        { return this.item.getValues(); }

        public String getString() throws RepositoryException
        { return this.item.getString(); }

        public InputStream getStream() throws RepositoryException
        { return this.item.getStream(); }

        public Binary getBinary() throws RepositoryException
        { return this.item.getBinary(); }

        public long getLong() throws RepositoryException
        { return this.item.getLong(); }

        public double getDouble() throws RepositoryException
        { return this.item.getDouble(); }

        public BigDecimal getDecimal() throws RepositoryException
        { return this.item.getDecimal(); }

        public Calendar getDate() throws RepositoryException
        { return this.item.getDate(); }

        public boolean getBoolean() throws RepositoryException
        { return this.item.getBoolean(); }

        public long getLength() throws RepositoryException
        { return this.item.getLength(); }

        public long[] getLengths() throws RepositoryException
        { return this.item.getLengths();}

        public PropertyDefinition getDefinition() throws RepositoryException
        { return this.item.getDefinition(); }

        public int getType() throws RepositoryException
        { return this.item.getType(); }

        public boolean isMultiple() throws RepositoryException
        { return this.item.isMultiple(); }
    }

    private final class NodeIteratorProxy implements NodeIterator {

        final NodeIterator nodeIterator;
        NodeProxy parent;

        NodeIteratorProxy(NodeIterator nodeIterator) {
            this.nodeIterator = nodeIterator;
        }

        NodeIteratorProxy(NodeIterator nodeIterator, NodeProxy parent) {
            this(nodeIterator);
            this.parent = parent;
        }

        public Node nextNode() {
            if (this.parent != null) {
                return new NodeProxy(this.nodeIterator.nextNode(), this.parent);
            } else {
                return new NodeProxy(this.nodeIterator.nextNode());
            }
        }

        public void skip(long l) {
            this.nodeIterator.skip(l);
        }

        public long getSize() {
            return this.nodeIterator.getSize();
        }

        public long getPosition() {
            return this.nodeIterator.getPosition();
        }

        public boolean hasNext() {
            return this.nodeIterator.hasNext();
        }

        public Object next() {
            Object nextObj = this.nodeIterator.next();
            if (nextObj instanceof Node) {
                if (this.parent != null) {
                    return new NodeProxy((Node) nextObj, this.parent);
                } else {
                    return new NodeProxy((Node) nextObj);
                }
            } else if (nextObj != null) {
                throw new IllegalStateException("next() returned non-Node");
            } else {
                return null;
            }
        }

        public void remove() {
            this.nodeIterator.remove();
        }
    }

    private final class PropertyIteratorProxy implements PropertyIterator {
        final PropertyIterator propertyIterator;
        NodeProxy parent;

        PropertyIteratorProxy(PropertyIterator propertyIterator) {
            this.propertyIterator = propertyIterator;
        }

        PropertyIteratorProxy(PropertyIterator propertyIterator, NodeProxy parent) {
            this.propertyIterator = propertyIterator;
            this.parent = parent;
        }

        public Property nextProperty() {
            return new PropertyProxy(this.propertyIterator.nextProperty(), this.parent);
        }

        public void skip(long l) {
            this.propertyIterator.skip(l);
        }

        public long getSize() {
            return this.propertyIterator.getSize();
        }

        public long getPosition() {
            return this.propertyIterator.getPosition();
        }

        public boolean hasNext() {
            return this.propertyIterator.hasNext();
        }

        public Object next() {
            Object nextObj = this.propertyIterator.next();
            if (nextObj instanceof Property) {
                if (this.parent != null) {
                    return new PropertyProxy((Property) nextObj, this.parent);
                } else {
                    return new PropertyProxy((Property) nextObj);
                }
            } else if (nextObj != null) {
                throw new IllegalStateException("next() returned non-Property");
            } else {
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not implemented");
        }
    }

    //------------------------------------------------------------
    // Simple Proxied Session methods
    //------------------------------------------------------------
    public Repository getRepository()
    { return getSession().getRepository(); }

    public String getUserID()
    { return getSession().getUserID(); }

    public String[] getAttributeNames()
    { return getSession().getAttributeNames(); }

    public Object getAttribute(String name)
    { return getSession().getAttribute(name); }

    public Workspace getWorkspace()
    { return getSession().getWorkspace(); }

    public Session impersonate(Credentials credentials) throws RepositoryException
    { return getSession().impersonate(credentials); }

    public boolean itemExists(String path) throws RepositoryException
    { return getSession().itemExists(path); }

    public boolean nodeExists(String path) throws RepositoryException
    { return getSession().nodeExists(path); }

    public boolean propertyExists(String path) throws RepositoryException
    { return getSession().propertyExists(path); }

    public boolean hasPendingChanges() throws RepositoryException
    { return getSession().hasPendingChanges(); }

    public ValueFactory getValueFactory() throws RepositoryException
    { return getSession().getValueFactory(); }

    public boolean hasPermission(String s, String s2) throws RepositoryException
    { return getSession().hasPermission(s, s2); }

    public void checkPermission(String s, String s2) throws AccessControlException, RepositoryException
    { getSession().checkPermission(s, s2); }

    public boolean hasCapability(String s, Object o, Object[] objects) throws RepositoryException
    { return getSession().hasCapability(s, o, objects); }

    public void exportSystemView(String s, ContentHandler contentHandler, boolean b, boolean b2)
            throws SAXException, RepositoryException
    { getSession().exportSystemView(s, contentHandler, b, b2); }

    public void exportSystemView(String s, OutputStream outputStream, boolean b, boolean b2)
            throws IOException, RepositoryException
    { getSession().exportSystemView(s, outputStream, b, b2); }

    public void exportDocumentView(String s, ContentHandler contentHandler, boolean b, boolean b2)
            throws SAXException, RepositoryException
    { getSession().exportDocumentView(s, contentHandler, b, b2); }

    public void exportDocumentView(String s, OutputStream outputStream, boolean b, boolean b2)
            throws IOException, RepositoryException
    { getSession().exportDocumentView(s, outputStream, b, b2); }

    public String[] getNamespacePrefixes() throws RepositoryException
    { return getSession().getNamespacePrefixes(); }

    public String getNamespaceURI(String s) throws RepositoryException
    { return getSession().getNamespaceURI(s); }

    public String getNamespacePrefix(String s) throws RepositoryException
    { return getSession().getNamespacePrefix(s); }

    public void logout()
    { getSession().logout(); }

    public boolean isLive()
    { return getSession().isLive(); }

    public void addLockToken(String s)
    { getSession().addLockToken(s); }

    public void removeLockToken(String s)
    { getSession().removeLockToken(s);}

    public String[] getLockTokens()
    { return getSession().getLockTokens(); }

    public AccessControlManager getAccessControlManager() throws RepositoryException
    { return getSession().getAccessControlManager(); }

    public RetentionManager getRetentionManager() throws RepositoryException
    { return getSession().getRetentionManager(); }
}
