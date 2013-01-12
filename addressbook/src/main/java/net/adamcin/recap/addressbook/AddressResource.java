package net.adamcin.recap.addressbook;

import net.adamcin.recap.api.RecapAddress;
import org.apache.sling.api.resource.Resource;

/**
 * @author madamcin
 * @version $Id: AddressResource.java$
 */
public interface AddressResource extends RecapAddress {

    public static final String RT_ADDRESS = "recap/components/addressbook/address";

    Resource getResource();
}
