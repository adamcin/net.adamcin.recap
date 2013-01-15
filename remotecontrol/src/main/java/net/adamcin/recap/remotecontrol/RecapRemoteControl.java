package net.adamcin.recap.remotecontrol;

import net.adamcin.recap.api.RecapAddress;

import java.util.Iterator;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRemoteControl.java$
 */
public interface RecapRemoteControl {

    Iterator<RecapPath> listRemotePaths(RecapAddress address, RecapRequest request)
            throws RecapRemoteException;

    List<RecapStrategyDescriptor> listRemoteStrategies(RecapAddress address)
            throws RecapRemoteException;

    List<RecapStrategyDescriptor> listLocalStrategies();

    RecapStrategy getStrategy(String strategyType);

    void ungetStrategy(RecapStrategy strategy);

}
