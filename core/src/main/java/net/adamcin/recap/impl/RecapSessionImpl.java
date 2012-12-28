package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import net.adamcin.recap.Recap;
import net.adamcin.recap.RecapConstants;
import net.adamcin.recap.RecapPath;
import net.adamcin.recap.RecapRemoteContext;
import net.adamcin.recap.RecapSession;
import net.adamcin.recap.RecapSessionException;
import net.adamcin.recap.RecapStrategyException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author madamcin
 * @version $Id: RecapSessionImpl.java$
 */
public class RecapSessionImpl implements RecapSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapSessionImpl.class);

    private final Recap recap;
    private final RecapRemoteContext context;
    private final Session localSession;
    private final Session sourceSession;


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

    private void track(String path, String fmt, Object[] args) {
        if (this.tracker != null) {
            this.tracker.onMessage(ProgressTrackerListener.Mode.TEXT, String.format(fmt, args), path);
        }
    }

public void copy(RecapPath path) throws RecapSessionException {
        track("", "# Copy %s from %s://%s:%d/",
                new Object[] { path.getLeaf().getJcrPath(),
                this.remoteAddress.getURI().getScheme(),
                this.remoteAddress.getURI().getHost(),
                this.remoteAddress.getURI().getPort() });

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
            LOGGER.debug("CaponeStrategyError while preparing path: {}. Message: {}", path, e.getMessage());
            track(path.getLeaf().getJcrPath(), "------ E %s", new Object[] { e.getMessage() });
        } catch (RepositoryException e) {
            LOGGER.error("RepositoryException while copying path: {}. Message: {}", path, e.getMessage());
            track(path.getLeaf().getJcrPath(), "------ F %s", new Object[] { e.getMessage() });
            throw new RecapSessionException("RepositoryException while preparing path: " + path, e);
        }
    }

    public void doCopy() throws RecapSessionException, RecapStrategyException {
        Iterator<RecapPath> paths = recap.listRemotePaths(getContext());

        if (paths != null) {
            while (paths.hasNext()) {

            }
        }
    }

    public void setTracker(ProgressTrackerListener tracker) {
        this.tracker = tracker;
    }

    public ProgressTrackerListener getTracker() {
        return this.tracker;
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


}
