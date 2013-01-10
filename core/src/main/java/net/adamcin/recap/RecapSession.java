package net.adamcin.recap;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;

import javax.jcr.Session;

/**
 * @author madamcin
 * @version $Id: RecapSession.java$
 */
public interface RecapSession {

    void doCopy() throws RecapException;

    void logout();

    void setTracker(ProgressTrackerListener tracker);

    ProgressTrackerListener getTracker();

    RecapSessionContext getContext();

    int getBatchSize();

    void setBatchSize(int batchSize);

    long getThrottle();

    void setThrottle(long throttle);

    boolean getOnlyNewer();

    void setOnlyNewer(boolean onlyNewer);

    boolean getUpdate();

    void setUpdate(boolean update);

    Session getLocalSession();

    //-----------------------------------------------------------------
    // Getters for Session Statistics
    //-----------------------------------------------------------------
    int getTotalRecapPaths();

    RecapPath getLastSuccessfulRecapPath();

    int getTotalNodes();

    long getTotalSize();

    long getTotalTimeMillis();
}
