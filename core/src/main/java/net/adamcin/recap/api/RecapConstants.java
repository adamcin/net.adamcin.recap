package net.adamcin.recap.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class hosting several important Recap API constants
 * @author madamcin
 * @version $Id: RecapConstants.java$
 */
public class RecapConstants {

    private RecapConstants() {
        // prevent instantiation
    }

    /**
     * Paths to the core Recap servlets
     */
    public static final String SERVLET_COPY_PATH = "/bin/recap/copy";
    public static final String SERVLET_LIST_PATH = "/bin/recap/list";
    public static final String SERVLET_STRATEGIES_PATH = "/bin/recap/strategies";

    /**
     * ComponentFactory interface
     */
    public static final String STRATEGY_FACTORY = "net.adamcin.recap.RecapStrategy";

    public static final String KEY_STRATEGY_TYPE = "type";
    public static final String KEY_STRATEGY_LABEL = "label";
    public static final String KEY_STRATEGY_DESCRIPTION = "description";

    public static final String DIRECT_STRATEGY = "direct";

    public static final int DEFAULT_DEFAULT_PORT = 4502;
    public static final String DEFAULT_DEFAULT_CONTEXT_PATH = "";
    public static final String DEFAULT_DEFAULT_USERNAME = "anonymous";
    public static final String DEFAULT_DEFAULT_PASSWORD = "anonymous";
    public static final int DEFAULT_DEFAULT_BATCH_SIZE = 1024;
    public static final String DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY = "cq:lastModified";

    // ------------------------------------------------
    // Request Parameters related to RecapAddresses
    // ------------------------------------------------
    public static final String RP_HOSTNAME = ":host";
    public static final String RP_PORT = ":port";
    public static final String RP_IS_HTTPS = ":is_https";
    public static final String RP_USERNAME = ":user";
    public static final String RP_PASSWORD = ":pass";
    public static final String RP_CONTEXT_PATH = ":context_path";

    // ------------------------------------------------
    // Request Parameters related to RecapRequests
    // ------------------------------------------------
    public static final String RP_STRATEGY = ":strategy";
    public static final String RP_SELECTORS = ":selectors";
    public static final String RP_SUFFIX = ":suffix";

    // ------------------------------------------------
    // Request Parameters related to RecapOptions
    // ------------------------------------------------
    public static final String RP_UPDATE = ":update";
    public static final String RP_ONLY_NEWER = ":only_newer";
    public static final String RP_BATCH_SIZE = ":batch_size";
    public static final String RP_THROTTLE = ":throttle";

}
