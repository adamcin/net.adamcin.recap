package net.adamcin.recap.addressbook;

import net.adamcin.recap.api.RecapAddress;
import org.apache.sling.api.resource.Resource;

/**
 * @author madamcin
 * @version $Id: Address.java$
 */
public interface Address extends RecapAddress {

    Resource getResource();
}
