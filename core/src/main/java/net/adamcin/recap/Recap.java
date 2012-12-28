package net.adamcin.recap;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Iterator;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    RecapSession initSession(ResourceResolver resourceResolver,
                             RecapRemoteContext context)
            throws RecapSessionException;

    Iterator<RecapPath> listRemotePaths(RecapRemoteContext context)
            throws RecapStrategyException;

    List<String> listRemoteStrategies(RecapRemoteContext context)
            throws RecapStrategyException;

    List<String> listLocalStrategies();

    RecapStrategy getStrategy(String strategyType);

    void ungetStrategy(RecapStrategy strategy);

    void interruptSessions();

    void clearSessionInterrupt();
}
