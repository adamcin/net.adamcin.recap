package net.adamcin.recap.impl;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi2davex.BatchReadConfig;

import javax.jcr.NamespaceException;

/**
 * @author madamcin
 * @version $Id: RecapBatchReadConfig.java$
 */
public class RecapBatchReadConfig implements BatchReadConfig {

    public int getDepth(Path path, PathResolver resolver) throws NamespaceException {
        String jcrPath = resolver.getJCRPath(path);
        if ("/".equals(jcrPath)) {
            return 2;
        } else if ("/jcr:system".equals(jcrPath)) {
            return 1;
        } else {
            return 4;
        }
    }
}
