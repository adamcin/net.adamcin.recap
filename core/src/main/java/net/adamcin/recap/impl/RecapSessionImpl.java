package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.fs.api.WorkspaceFilter;
import com.day.jcr.vault.util.JcrConstants;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapConstants;
import net.adamcin.recap.api.RecapException;
import net.adamcin.recap.api.RecapOptions;
import net.adamcin.recap.api.RecapPath;
import net.adamcin.recap.api.RecapRequest;
import net.adamcin.recap.api.RecapSession;
import net.adamcin.recap.api.RecapSessionException;
import net.adamcin.recap.api.RecapRemoteException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author madamcin
 * @version $Id: RecapSessionImpl.java$
 */
public class RecapSessionImpl implements RecapSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapSessionImpl.class);

    enum PathAction {
        ADD("A"), UPDATE("U"), DELETE("D"), NO_ACTION("-");

        String action;
        PathAction(String action) {
            this.action = action;
        }

        String getAction() {
            return action;
        }
    }

    private final RecapImpl recap;
    private final RecapAddress address;
    private final RecapRequest request;
    private final RecapOptions options;
    private final Session localSession;
    private final Session sourceSession;

    private WorkspaceFilter filter;
    private ProgressTrackerListener tracker;

    private RecapPath lastSuccessfulPath;
    private int totalRecapPaths = 0;
    private int numNodes = 0;
    private int totalNodes = 0;
    private long totalSize = 0L;
    private long currentSize = 0L;
    private long start = 0L;
    private long end = 0L;

    private Map<String, String> prefixMapping = new HashMap<String, String>();
    private boolean allowLastModifiedProperty = false;
    private boolean interrupted = false;
    private boolean finished = false;


    public RecapSessionImpl(RecapImpl recap,
                            RecapAddress address,
                            RecapRequest request,
                            RecapOptions options,
                            Session localSession,
                            Session sourceSession)
            throws RecapSessionException {

        this.recap = recap;
        this.address = address;
        this.request = request;
        this.options = options;
        this.localSession = localSession;
        this.sourceSession = sourceSession;

        allowLastModifiedProperty = isValidNameForSession(this.options.getLastModifiedProperty());
    }

    public RecapAddress getAddress() {
        return address;
    }

    public RecapRequest getRequest() {
        return request;
    }

    public RecapOptions getOptions() {
        return options;
    }

    public boolean isFinished() {
        return finished;
    }

    public void logout() {
        if (this.sourceSession != null) {
            this.sourceSession.logout();
        }
    }

    public WorkspaceFilter getFilter() {
        return filter;
    }

    public void setFilter(WorkspaceFilter filter) {
        this.filter = filter;
    }

    public ProgressTrackerListener getTracker() {
        return tracker;
    }

    public void setTracker(ProgressTrackerListener tracker) {
        this.tracker = tracker;
    }

    private void trackFilterIgnore(String path) {
        if (this.getTracker() != null) {
            String formatted = String.format("-------- %s (Ignored by filter)", path);
            this.getTracker().onMessage(ProgressTrackerListener.Mode.TEXT, "I", formatted);
        }
    }

    private void trackPath(PathAction action, String path) {
        if (this.getTracker() != null) {
            String formatted = String.format("%08d %s", ++this.totalNodes, path);
            this.getTracker().onMessage(ProgressTrackerListener.Mode.TEXT, action.getAction(), formatted);
        }
    }

    private void trackError(String path, Exception exception) {
        if (this.getTracker() != null) {
            String formatted = String.format("-------- %s (%s)", path, exception.getMessage());
            this.getTracker().onMessage(ProgressTrackerListener.Mode.TEXT, "E", formatted);
        }
    }

    private void trackFailure(String path, Exception exception) {
        if (this.getTracker() != null) {
            String formatted = String.format("-------- %s (%s)", path, exception.getMessage());
            this.getTracker().onMessage(ProgressTrackerListener.Mode.TEXT, "F", formatted);
        }
    }

    private void trackMessage(String fmt, Object... args) {
        if (this.getTracker() != null) {
            this.getTracker().onMessage(ProgressTrackerListener.Mode.TEXT, "M", String.format(fmt, args));
        }
    }

    private boolean isDirect() {
        return StringUtils.isEmpty(getRequest().getStrategy()) ||
               RecapConstants.DIRECT_STRATEGY.equals(getRequest().getStrategy());
    }

    public void doCopy() throws RecapException {
        if (this.finished) {
            throw new RecapSessionException("RecapSession already finished.");
        }

        RecapException exception = null;
        try {
            Iterator<RecapPath> paths;
            if (isDirect()) {
                try {
                    Node directNode = sourceSession.getNode(getRequest().getSuffix());
                    paths = Arrays.asList( RecapPath.build(directNode) ).iterator();
                } catch (RepositoryException e) {
                    throw new RecapRemoteException(e);
                }
            } else {
                paths = recap.listRemotePaths(getAddress(), getRequest());
            }

            if (paths != null) {
                while (paths.hasNext()) {
                    copy(paths.next());
                }
            }
        } catch (RecapException e) {
            exception = e;
            this.interrupted = true;
        }

        finish();

        if (exception != null) {
            throw exception;
        }
    }

    public void copy(RecapPath path) throws RecapSessionException {
        if (recap.sessionsInterrupted) {
            throw new RecapSessionException("RecapSession interrupted.");
        }

        trackMessage("Copy %s from http://%s:%d/", path.getLeaf().getJcrPath(), this.address.getHostname(), this.address.getPort());

        try {
            int result = path.getLeaf().establishPath(getLocalSession());
            this.numNodes += result;
            this.totalNodes += result;

            Node srcNode = path.getNode(this.getSourceSession());
            Node dstParent = path.getParentNode(this.getLocalSession());
            String dstName = path.getName();

            this.copy(srcNode, dstParent, dstName, true);
            this.lastSuccessfulPath = path;
            this.totalRecapPaths++;
        } catch (PathNotFoundException e) {
            LOGGER.debug("PathNotFoundException while preparing path: {}. Message: {}", path, e.getMessage());
            trackError(path.getLeaf().getJcrPath(), e);
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while copying path: {}. Message: {}", path, e.getMessage());
            trackFailure(path.getLeaf().getJcrPath(), e);
            throw new RecapSessionException("RepositoryException while preparing path: " + path, e);
        }
    }

    private void finish() throws RecapSessionException {
        this.finished = true;
        RecapSessionException exception = null;
        if (!this.interrupted && this.numNodes > 0) {
            trackMessage("Saving %d nodes...", this.numNodes);
            try {
                this.getLocalSession().save();
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
                    this.getTotalRecapPaths(), this.getLastSuccessfulRecapPath());
        }

        if (exception != null) {
            throw exception;
        }
    }

    private void copy(Node src, Node dstParent, String dstName, boolean recursive) throws RepositoryException {
        String path = src.getPath();
        String dstPath = dstParent.getPath() + "/" + dstName;
        if ((this.getFilter() != null) && (!this.getFilter().contains(path))) {
            trackFilterIgnore(path);
            return;
        }

        boolean useSysView = src.getDefinition().isProtected();

        boolean isNew = false;
        boolean overwrite = this.options.isUpdate();
        Node dst;
        if (dstParent.hasNode(dstName)) {
            dst = dstParent.getNode(dstName);
            if (overwrite) {
                if ((this.options.isOnlyNewer()) && (dstName.equals(JcrConstants.JCR_CONTENT))) {
                    if (isNewer(src, dst)) {
                        trackPath(PathAction.UPDATE, dstPath);
                    } else {
                        overwrite = false;
                        recursive = false;
                        trackPath(PathAction.NO_ACTION, dstPath);
                    }
                } else {
                    trackPath(PathAction.UPDATE, dstPath);
                }

                if (useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                }
            } else {
                trackPath(PathAction.NO_ACTION, dstPath);
            }
        } else {
            try {
                if (useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                } else {
                    dst = dstParent.addNode(dstName, src.getPrimaryNodeType().getName());
                }
                trackPath(PathAction.ADD, dstPath);
                isNew = true;
            } catch (RepositoryException e) {
                LOGGER.warn("Error while adding node {} (ignored): {}", dstPath, e.toString());
                return;
            }
        }
        if (useSysView) {
            trackTree(dst, isNew);
        } else {
            Set<String> names = new HashSet<String>();
            if ((overwrite) || (isNew)) {
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
                    }
                }
            }

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

                for (String name : names) {
                    try {
                        Node cNode = dst.getNode(name);
                        trackPath(PathAction.DELETE, cNode.getPath());
                        cNode.remove();
                    } catch (RepositoryException e) {
                    }
                }
            }
        }

        if (++this.numNodes >= this.getOptions().getBatchSize())
            try {
                trackMessage("Intermediate saving %d nodes (%d kB)...", this.numNodes, this.currentSize / 1000L);
                long now = System.currentTimeMillis();
                this.localSession.save();
                long end = System.currentTimeMillis();
                trackMessage("Done in %d ms. Total time: %d, total nodes %d, %d kB", end - now, end - this.start,
                        this.totalNodes, this.totalSize / 1000L);
                this.numNodes = 0;
                this.currentSize = 0L;
                if (this.options.getThrottle() > 0L) {
                    trackMessage("Throttling enabled. Waiting %d second%s...", this.options.getThrottle(), this.options.getThrottle() == 1L ? "" : "s");
                    try {
                        Thread.sleep(this.options.getThrottle() * 1000L);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error during intermediate save ({}); try again later: {}", this.numNodes, e.toString());
            }
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
                trackPath(PathAction.ADD, child.getPath());
            } else {
                trackPath(PathAction.UPDATE, child.getPath());
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
                String uri = this.sourceSession.getNamespaceURI(prefix);
                int i = -1;
                try {
                    mapped = this.localSession.getNamespacePrefix(uri);
                } catch (NamespaceException e) {
                    mapped = prefix;
                    i = 0;
                }

                while (i >= 0) {
                    try {
                        this.localSession.getWorkspace().getNamespaceRegistry().registerNamespace(mapped, uri);
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

    public Session getSourceSession() {
        return this.sourceSession;
    }

    public Session getLocalSession() {
        return this.localSession;
    }

    public int getTotalRecapPaths() {
        return this.totalRecapPaths;
    }

    public RecapPath getLastSuccessfulRecapPath() {
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

}
