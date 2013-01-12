package net.adamcin.recap.api;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.fs.api.WorkspaceFilter;

import javax.jcr.Session;

/**
 * @author madamcin
 * @version $Id: RecapSession.java$
 */
public interface RecapSession {

    void doCopy() throws RecapException;

    void logout();

    Session getLocalSession();

    RecapOptions getOptions();

    RecapAddress getAddress();

    RecapRequest getRequest();

    WorkspaceFilter getFilter();

    void setFilter(WorkspaceFilter filter);

    ProgressTrackerListener getTracker();

    void setTracker(ProgressTrackerListener tracker);

    //-----------------------------------------------------------------
    // Getters for Session Statistics
    //-----------------------------------------------------------------
    boolean isFinished();

    int getTotalRecapPaths();

    RecapPath getLastSuccessfulRecapPath();

    int getTotalNodes();

    long getTotalSize();

    long getTotalTimeMillis();
}
