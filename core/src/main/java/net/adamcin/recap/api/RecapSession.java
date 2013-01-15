package net.adamcin.recap.api;

import javax.jcr.Session;

/**
 * @author madamcin
 * @version $Id: RecapSession.java$
 */
public interface RecapSession {

    void remoteCopy(String rootPath) throws RecapSessionException;

    void finish() throws RecapSessionException;

    void logout();

    Session getLocalSession();

    RecapOptions getOptions();

    RecapProgressListener getProgressListener();

    void setProgressListener(RecapProgressListener progressListener);

    //-----------------------------------------------------------------
    // Getters for Session Statistics
    //-----------------------------------------------------------------
    boolean isFinished();

    int getTotalRecapPaths();

    String getLastSuccessfulRecapPath();

    int getTotalNodes();

    long getTotalSize();

    long getTotalTimeMillis();
}
