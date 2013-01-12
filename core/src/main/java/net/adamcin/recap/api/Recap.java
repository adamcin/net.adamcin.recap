package net.adamcin.recap.api;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Iterator;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    RecapSession initSession(ResourceResolver resourceResolver, RecapAddress address, RecapRequest request, RecapOptions options)
            throws RecapSessionException;

    Iterator<RecapPath> listRemotePaths(RecapAddress address, RecapRequest request)
            throws RecapRemoteException;

    List<RecapStrategyDescriptor> listRemoteStrategies(RecapAddress address)
            throws RecapRemoteException;

    List<RecapStrategyDescriptor> listLocalStrategies();

    RecapStrategy getStrategy(String strategyType);

    void ungetStrategy(RecapStrategy strategy);

    // -----------------------------------------
    // expose non-private default recap parameters
    // -----------------------------------------

    int getDefaultPort();

    String getDefaultContextPath();

    String getDefaultUsername();

    int getDefaultBatchSize();

    String getDefaultLastModifiedProperty();
}
