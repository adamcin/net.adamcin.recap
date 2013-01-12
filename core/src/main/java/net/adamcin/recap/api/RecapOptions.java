package net.adamcin.recap.api;

import com.day.jcr.vault.fs.api.ProgressTrackerListener;
import com.day.jcr.vault.fs.api.WorkspaceFilter;

/**
 * @author madamcin
 * @version $Id: RecapOptions.java$
 */
public interface RecapOptions {


    String getLastModifiedProperty();

    int getBatchSize();

    long getThrottle();

    boolean isOnlyNewer();

    boolean isUpdate();

}
