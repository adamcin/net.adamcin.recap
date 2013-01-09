package net.adamcin.recap;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;

import javax.jcr.Session;
import java.io.Closeable;

/**
 * @author madamcin
 * @version $Id: RecapSession.java$
 */
public interface RecapSession extends Closeable {

    void doCopy() throws RecapSessionException, RecapSourceException;

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

    Session getSourceSession() throws RecapSessionException;

    Session getLocalSession() throws RecapSessionException;

    //-----------------------------------------------------------------
    // Getters for Session Statistics
    //-----------------------------------------------------------------
    int getTotalRecapPaths();

    RecapPath getLastSuccessfulRecapPath();

    int getTotalNodes();

    long getTotalSize();

    long getTotalTimeMillis();
}
