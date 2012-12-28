package net.adamcin.recap;

import org.apache.sling.api.SlingHttpServletRequest;

import javax.jcr.Node;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: RecapStrategy.java$
 */
public interface RecapStrategy {

    Iterator<Node> listNodes(SlingHttpServletRequest recapRequest)
            throws RecapStrategyException;
}
