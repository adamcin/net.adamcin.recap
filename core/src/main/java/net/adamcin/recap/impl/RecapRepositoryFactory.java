package net.adamcin.recap.impl;

import com.day.jcr.vault.fs.api.RepositoryAddress;
import com.day.jcr.vault.fs.api.RepositoryFactory;
import org.apache.jackrabbit.client.RepositoryFactoryImpl;
import org.apache.jackrabbit.jcr2spi.Jcr2spiRepositoryFactory;
import org.apache.jackrabbit.spi.commons.logging.Slf4jLogWriterProvider;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;
import org.apache.jackrabbit.spi2davex.Spi2davexRepositoryServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author madamcin
 * @version $Id: RecapRepositoryFactory.java$
 */
public class RecapRepositoryFactory implements RepositoryFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapRepositoryFactory.class);

    private static final Set<String> SCHEMES =
            Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("http", "https")));

    private static final BatchReadConfig DEFAULT_BATCH_READ_CONFIG = new RecapBatchReadConfig();

    public Set<String> getSupportedSchemes() {
        return SCHEMES;
    }

    public Repository createRepository(RepositoryAddress address) throws RepositoryException {
        if (!SCHEMES.contains(address.getSpecificURI().getScheme())) {
            return null;
        }

        URI uri = address.getSpecificURI();

        if (uri.getUserInfo() != null) {
            try {
                uri = new URI(
                        uri.getScheme(),
                        null,
                        uri.getHost(),
                        uri.getPort(),
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment());
            } catch (URISyntaxException ignored) { }
        }

        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Jcr2spiRepositoryFactory.PARAM_REPOSITORY_SERVICE_FACTORY, Spi2davexRepositoryServiceFactory.class.getName());
        params.put(Jcr2spiRepositoryFactory.PARAM_ITEM_CACHE_SIZE, 128);
        params.put(Jcr2spiRepositoryFactory.PARAM_LOG_WRITER_PROVIDER, new Slf4jLogWriterProvider());
        params.put(Spi2davexRepositoryServiceFactory.PARAM_BATCHREAD_CONFIG, DEFAULT_BATCH_READ_CONFIG);
        params.put(Spi2davexRepositoryServiceFactory.PARAM_REPOSITORY_URI, uri.toString());

        return new RepositoryFactoryImpl().getRepository(params);
    }
}
