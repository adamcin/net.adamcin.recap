package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import com.day.jcr.vault.fs.api.RepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.spi.ServiceRegistry;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * @author madamcin
 * @version $Id: RecapRepositoryProvider.java$
 */
public class RecapRepositoryProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapRepositoryProvider.class);

    private Map<RepositoryAddress, Repository> repos = new HashMap<RepositoryAddress, Repository>();

    public Repository getRepository(RepositoryAddress address)
            throws RepositoryException {
        Repository rep = this.repos.get(address);
        if (rep instanceof RecapRepositoryAddress || rep == null) {
            rep = createRepository(address);
            this.repos.put(address, rep);
        }
        return rep;
    }

    private Repository createRepository(RepositoryAddress address)
            throws RepositoryException {
        Iterator<RepositoryFactory> iter = ServiceRegistry.lookupProviders(
                RepositoryFactory.class, getClass().getClassLoader());
        Set<String> supported = new HashSet<String>();
        while (iter.hasNext()) {
            RepositoryFactory fac = iter.next();
            supported.addAll(fac.getSupportedSchemes());

            Repository rep = null;
            ClassLoader orig = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                rep = fac.createRepository(address);
            } finally {
                Thread.currentThread().setContextClassLoader(orig);
            }

            if (rep != null) {
                if ((Boolean.getBoolean("jcrlog.sysout"))
                        || (System.getProperty("jcrlog.file") != null)) {
                    Repository wrapped = wrapLogger(rep, address);
                    if (wrapped != null) {
                        LOGGER.info("Enabling JCR Logger.");
                        rep = wrapped;
                    }
                }
                return rep;
            }
        }
        StringBuilder msg = new StringBuilder("URL scheme ");
        msg.append(address.getURI().getScheme());
        msg.append(" not supported. only");
        for (String s : supported) {
            msg.append(" ").append(s);
        }
        throw new RepositoryException(msg.toString());
    }

    @SuppressWarnings("rawtypes")
    private Repository wrapLogger(Repository base, RepositoryAddress address) {
        try {
            Class clazz = getClass().getClassLoader().loadClass(
                    "org.apache.jackrabbit.jcrlog.RepositoryLogger");

            Properties props = new Properties();
            for (Object o : System.getProperties().keySet()) {
                String name = o.toString();
                if (name.startsWith("jcrlog.")) {
                    props.put(name.substring("jcrlog.".length()),
                            System.getProperty(name));
                }
            }
            Constructor c = clazz.getConstructor(new Class[] {
                    Repository.class, Properties.class, String.class });
            return (Repository) c.newInstance(base, props, address.toString());
        } catch (Exception e) {
            LOGGER.error("Unable to initialize JCR logger: {}", e.toString());
        }
        return null;
    }
}
