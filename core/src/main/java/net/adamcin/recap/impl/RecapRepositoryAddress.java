package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import net.adamcin.recap.RecapConstants;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author madamcin
 * @version $Id: RecapRepositoryAddress.java$
 */
public class RecapRepositoryAddress extends RepositoryAddress implements Repository {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecapRepositoryAddress.class);

    final ResourceResolver resolver;
    final RepositoryAddress delegate;

    public RecapRepositoryAddress(ResourceResolver resolver)
            throws URISyntaxException {
        this(resolver, null);
    }

    public RecapRepositoryAddress(ResourceResolver resolver, RepositoryAddress delegate)
            throws URISyntaxException {
        super(getBasicURI());
        this.resolver = resolver;
        if (delegate != null) {
            this.delegate = delegate;
        } else {
            this.delegate = new RepositoryAddress(getBasicURI());
        }
    }

    /**
     * The basic URI is used for initializing the wrapper address, so it is nothing more than
     * the root path. The URI used for operations belongs to the WRAPPED address.
     * @return
     */
    private static String getBasicURI() {
        return RecapConstants.SCHEME + ":/";
    }

    /**
     * The wrapper's resolve method delegates the resolution to create a new,
     * proper RepositoryAddress that is passed to the constructor of a new
     * wrapper.
     */
    @Override
    public RepositoryAddress resolve(String path) {
        RepositoryAddress address = this.delegate.resolve(path);
        try {
            return new RecapRepositoryAddress(this.resolver, address);
        } catch (URISyntaxException e) {
            LOGGER.error("[resolve] Failed to create new address", e);
        }
        return null;
    }

    private RepositoryAddress getDelegate() {
        return this.delegate;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof RecapRepositoryAddress) {
            return this.delegate.equals( ((RecapRepositoryAddress) obj).getDelegate() );
        } else {
            return false;
        }
    }

    @Override
    public URI getURI() {
        return this.delegate.getURI();
    }

    @Override
    public String getPath() {
        return this.delegate.getPath();
    }

    @Override
    public String getWorkspace() {
        return this.delegate.getWorkspace();
    }

    @Override
    public URI getSpecificURI() {
        return this.delegate.getSpecificURI();
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    /**
     * This returns a set of dummy credentials because the login methods simply return the
     * original ResourceResolver's underlying Session object
     */
    @Override
    public Credentials getCredentials() {
        return new SimpleCredentials("capone", "non-working-credentials".toCharArray());
    }

    public String[] getDescriptorKeys() {
        return null;
    }

    public boolean isStandardDescriptor(String key) {
        return false;
    }

    public boolean isSingleValueDescriptor(String key) {
        return false;
    }

    public Value getDescriptorValue(String key) {
        return null;
    }

    public Value[] getDescriptorValues(String key) {
        return null;
    }

    public String getDescriptor(String key) {
        return null;
    }

    /**
     * This method completely overrides the Repository login method to simply
     * return the original ResourceResolver's underlying Session. All the other
     * login overloads defer to this method.
     */
    public Session login() throws javax.jcr.LoginException,
            RepositoryException {
        return this.resolver.adaptTo(Session.class);
    }

    public Session login(Credentials credentials, String workspaceName)
            throws javax.jcr.LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return login();
    }

    public Session login(Credentials credentials)
            throws javax.jcr.LoginException, RepositoryException {
        return login();
    }

    public Session login(String workspaceName)
            throws javax.jcr.LoginException, NoSuchWorkspaceException,
            RepositoryException {
        return login();
    }
}
