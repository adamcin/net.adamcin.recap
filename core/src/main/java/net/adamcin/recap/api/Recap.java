package net.adamcin.recap.api;

import javax.jcr.Session;

/**
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    RecapSession initSession(Session localJcrSession, RecapAddress address, RecapOptions options)
            throws RecapSessionException;


    // -----------------------------------------
    // expose non-private default recap parameters
    // -----------------------------------------

    int getDefaultPort();

    String getDefaultContextPath();

    String getDefaultUsername();

    String getDefaultPassword();

    int getDefaultBatchSize();

    String getDefaultLastModifiedProperty();

    String getDisplayableUrl(RecapAddress recapAddress);
}
