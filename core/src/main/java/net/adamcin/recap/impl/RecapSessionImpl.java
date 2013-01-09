package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.fs.api.WorkspaceFilter;
import com.day.jcr.vault.util.JcrConstants;
import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapRemoteContext;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import net.adamcin.recap.RecapStrategyException;
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
import java.io.IOException;
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

    private static final String TRACK_ADD = "%08d A";
    private static final String TRACK_UPDATE = "%08d U";
    private static final String TRACK_DELETE = "%08d D";
    private static final String TRACK_NO_ACTION = "%08d -";

    private final Recap recap;
    private final RecapRemoteContext context;
    private final Session localSession;
    private final Session sourceSession;

    private WorkspaceFilter filter;
    private String lastModifiedProperty;

    private ProgressTrackerListener tracker;
    private RecapPath lastSuccessfulPath;
    private int totalRecapPaths = 0;
    private int numNodes = 0;
    private int totalNodes = 0;
    private long totalSize = 0L;
    private long currentSize = 0L;
    private int batchSize = 1024;
    private long throttle = 0L;
    private long start = 0L;
    private long end = 0L;

    private boolean onlyNewer;
    private boolean update;

    private Map<String, String> prefixMapping = new HashMap<String, String>();
    private boolean interruptedByException = false;

    public RecapSessionImpl(Recap recap,
                            RecapRemoteContext context,
                            Session localSession,
                            Session sourceSession)
            throws RecapSessionException {

        this.recap = recap;
        this.context = context;
        this.localSession = localSession;
        this.sourceSession = sourceSession;
    }

    public RecapRemoteContext getContext() {
        return context;
    }

    public void close() throws IOException {
        if (this.sourceSession != null) {
            this.sourceSession.logout();
        }
    }

    private void track(String path, String fmt, Object... args) {
        if (this.tracker != null) {
            this.tracker.onMessage(ProgressTrackerListener.Mode.TEXT, String.format(fmt, args), path);
        }
    }

    public void copy(RecapPath path) throws RecapSessionException {
        track("", "# Copy %s from http://%s:%d/", path.getLeaf().getJcrPath(), this.context.getRemoteHost(), this.context.getRemotePort());

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
            track(path.getLeaf().getJcrPath(), "------ E %s", e.getMessage());
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while copying path: {}. Message: {}", path, e.getMessage());
            track(path.getLeaf().getJcrPath(), "------ F %s", e.getMessage());
            throw new RecapSessionException("RepositoryException while preparing path: " + path, e);
        }
    }

    public void doCopy() throws RecapSessionException, RecapStrategyException {
        Iterator<RecapPath> paths = recap.listRemotePaths(getContext());

        if (paths != null) {
            while (paths.hasNext()) {
                copy(paths.next());
            }
        }
    }

    public void setTracker(ProgressTrackerListener tracker) {
        this.tracker = tracker;
    }

    public ProgressTrackerListener getTracker() {
        return this.tracker;
    }

    public WorkspaceFilter getFilter() {
        return filter;
    }

    public void setFilter(WorkspaceFilter filter) {
        this.filter = filter;
    }

    public String getLastModifiedProperty() {
        return lastModifiedProperty;
    }

    public void setLastModifiedProperty(String lastModifiedProperty) {
        this.lastModifiedProperty = lastModifiedProperty;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getThrottle() {
        return this.throttle;
    }

    public void setThrottle(long throttle) {
        this.throttle = throttle;
    }

    public boolean getOnlyNewer() {
        return this.onlyNewer;
    }

    public void setOnlyNewer(boolean onlyNewer) {
        this.onlyNewer = onlyNewer;
    }

    public boolean getUpdate() {
        return this.update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public Session getSourceSession() throws RecapSessionException {
        return this.sourceSession;
    }

    public Session getLocalSession() throws RecapSessionException {
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

    /*
    private GetMethod getListMethod(SlingHttpServletRequest request, final String strategyType) {

        StringBuilder pathBuilder = new StringBuilder(RecapConstants.SERVLET_LIST_PATH);
        ResourceMetadata rm = CEMRequestPathInfo.toResourceMetadata(request.getRequestPathInfo());
        if (StringUtils.isNotEmpty(rm.getResolutionPathInfo())) {
            pathBuilder.append(rm.getResolutionPathInfo());
        }

        LOGGER.debug("[getListMethod] local URI: {}, QueryString: {}",
                request.getRequestURI(), request.getQueryString());

        QueryStringBuilder qsb = new QueryStringBuilder();
        qsb.addValue(RP_STRATEGY, strategyType);

        Enumeration<String> names = (Enumeration<String>) request.getParameterNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (!RESERVED_PARAMS.contains(name)) {
                    RequestParameter[] rpValues = request.getRequestParameters(name);
                    if (rpValues != null) {
                        for (RequestParameter rpValue : rpValues) {
                            if (rpValue.isFormField()) {
                                qsb.addRawValue(name, rpValue.getString());
                            }
                        }
                    }
                }
            }
        }

        pathBuilder.append(qsb.toString());
        LOGGER.debug("[getListMethod] remote URI: {}", pathBuilder.toString());

        final GetMethod getMethod = new GetMethod(pathBuilder.toString());
        return getMethod;
    }


    private HttpClient getClient(String rhost, String ruser, String rpass, int rport) throws URIException {
        HttpClient client = new HttpClient();

        client.getHostConfiguration().setHost(rhost, rport);
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(rhost, rport),
                new UsernamePasswordCredentials(ruser, rpass)
        );

        return client;
    }
    */

    private void copy(Node src, Node dstParent, String dstName, boolean recursive) throws RepositoryException {
        String path = src.getPath();
        String dstPath = dstParent.getPath() + "/" + dstName;
        if ((this.filter != null) && (!this.filter.contains(path))) {
            track(path, "------ I %s", "Ignored by filter");
            return;
        }

        boolean useSysView = src.getDefinition().isProtected();

        boolean isNew = false;
        boolean overwrite = this.update;
        Node dst;
        if (dstParent.hasNode(dstName)) {
            dst = dstParent.getNode(dstName);
            if (overwrite) {
                if ((this.onlyNewer) && (dstName.equals(JcrConstants.JCR_CONTENT))) {
                    if (isNewer(src, dst)) {
                        track(dstPath, TRACK_UPDATE, ++this.totalNodes);
                    } else {
                        overwrite = false;
                        recursive = false;
                        track(dstPath, TRACK_NO_ACTION, ++this.totalNodes);
                    }
                } else {
                    track(dstPath, TRACK_UPDATE, ++this.totalNodes);
                }

                if (useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                }
            } else {
                track(dstPath, TRACK_NO_ACTION, ++this.totalNodes);
            }
        } else {
            try {
                if (useSysView) {
                    dst = sysCopy(src, dstParent, dstName);
                } else {
                    dst = dstParent.addNode(dstName, src.getPrimaryNodeType().getName());
                }
                track(dstPath, TRACK_ADD, ++this.totalNodes);
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
                        track(cNode.getPath(), TRACK_DELETE, ++this.totalNodes);
                        cNode.remove();
                    } catch (RepositoryException e) {
                    }
                }
            }
        }

        if (++this.numNodes >= this.batchSize)
            try {
                track("", "# Intermediate saving %d nodes (%d kB)...", this.numNodes, this.currentSize / 1000L);
                long now = System.currentTimeMillis();
                this.localSession.save();
                long end = System.currentTimeMillis();
                track("", "# Done in %d ms. Total time: %d, total nodes %d, %d kB", end - now, end - this.start,
                        this.totalNodes, this.totalSize / 1000L);
                this.numNodes = 0;
                this.currentSize = 0L;
                if (this.throttle > 0L) {
                    track("", "# Throttling enabled. Waiting %d second%s...", this.throttle, this.throttle == 1L ? "" : "s");
                    try {
                        Thread.sleep(this.throttle * 1000L);
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
                track(child.getPath(), TRACK_ADD, ++this.totalNodes);
            } else {
                track(child.getPath(), TRACK_UPDATE, ++this.totalNodes);
            }
            trackTree(child, isNew);
        }
    }

    private boolean isNewer(Node src, Node dst) {
        try {
            Calendar srcDate = null;
            Calendar dstDate = null;
            if ((this.lastModifiedProperty != null) && (src.hasProperty(this.lastModifiedProperty)) && (dst.hasProperty(this.lastModifiedProperty))) {
                srcDate = src.getProperty(this.lastModifiedProperty).getDate();
                dstDate = dst.getProperty(this.lastModifiedProperty).getDate();
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

    private String checkNameSpace(String name) {
        try {
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
        } catch (RepositoryException e) {
            LOGGER.error("Error processing namespace for {}: {}", name, e.toString());
        }
        return name;
    }
}
