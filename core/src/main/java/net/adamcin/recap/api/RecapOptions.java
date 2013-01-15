package net.adamcin.recap.api;

/**
 * @author madamcin
 * @version $Id: RecapOptions.java$
 */
public interface RecapOptions {


    String getLastModifiedProperty();

    Integer getBatchSize();

    Long getThrottle();

    boolean isOnlyNewer();

    boolean isUpdate();

}
