package net.adamcin.recap.impl;

import net.adamcin.recap.RecapStrategy;
import net.adamcin.recap.RecapStrategyException;
import org.apache.felix.scr.annotations.Component;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author madamcin
 * @version $Id: SinglePathRecapStrategy.java$
 */
@Component(factory = "net.adamcin.recap.RecapStrategy/single")
public class SinglePathRecapStrategy implements RecapStrategy {

    public Iterator<Node> listNodes(SlingHttpServletRequest recapRequest)
            throws RecapStrategyException {

        Resource resource = recapRequest.getResourceResolver().
                getResource(recapRequest.getRequestPathInfo().getSuffix());

        if (resource != null) {
            Node resourceNode = resource.adaptTo(Node.class);
            if (resourceNode != null) {
                return Arrays.asList(new Node[]{resourceNode}).iterator();
            }
        }
        return null;
    }
}
