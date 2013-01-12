package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapRemoteException;
import net.adamcin.recap.api.RecapStrategy;
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
@Component(factory = "net.adamcin.recap.RecapStrategy/single",
        metatype = true,
        label = "Single Path",
        description = "Recursively copy a single path from a remote CRX repository to this one.")
public class SinglePathRecapStrategy implements RecapStrategy {

    public Iterator<Node> listNodes(SlingHttpServletRequest recapRequest)
            throws RecapRemoteException {

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
