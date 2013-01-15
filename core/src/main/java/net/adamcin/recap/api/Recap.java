package net.adamcin.recap.api;

import org.apache.sling.api.resource.ResourceResolver;

/**
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    RecapSession initSession(ResourceResolver resourceResolver, RecapAddress address, RecapOptions options)
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
}
