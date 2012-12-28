package net.adamcin.recap;

import java.util.Queue;

/**
 * @author madamcin
 * @version $Id: RecapPathRequester.java$
 */
public interface RecapPathRequester {

    Queue<RecapPath> getPathQueue();

    void requestPaths();
}
