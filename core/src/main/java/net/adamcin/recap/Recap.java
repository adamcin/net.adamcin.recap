package net.adamcin.recap;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Iterator;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    RecapSession initSession(ResourceResolver resourceResolver, RecapSessionContext context)
            throws RecapSessionException;

    Iterator<RecapPath> listRemotePaths(RecapSessionContext context)
            throws RecapSourceException;

    List<RecapStrategyDescriptor> listRemoteStrategies(RecapSourceContext context)
            throws RecapSourceException;

    List<RecapStrategyDescriptor> listLocalStrategies();

    RecapStrategy getStrategy(String strategyType);

    void ungetStrategy(RecapStrategy strategy);

    void interruptSessions();

    void clearSessionInterrupt();

    int getDefaultRemotePort();

    String getDefaultRemoteUser();

}
