package net.adamcin.recap;

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

    public static final String DEFAULT_DEFAULT_REMOTE_STRATEGY = "single";

    public static final int DEFAULT_DEFAULT_REMOTE_PORT = 4502;
    public static final String DEFAULT_DEFAULT_REMOTE_USER = "recap";
    public static final String DEFAULT_DEFAULT_REMOTE_PASS = "recap";

    public static final String RP_REMOTE_HOST = ":host";
    public static final String RP_REMOTE_PORT = ":port";
    public static final String RP_REMOTE_IS_HTTPS = ":is_https";
    public static final String RP_REMOTE_USER = ":user";
    public static final String RP_REMOTE_PASS = ":pass";
    public static final String RP_REMOTE_CONTEXT_PATH = ":context_path";
    public static final String RP_REMOTE_STRATEGY = ":strategy";
    public static final String RP_INTERRUPT = ":interrupt";
    public static final String RP_UPDATE = ":update";
    public static final String RP_ONLY_NEWER = ":only_newer";
    public static final String RP_SELECTORS = ":selectors";
    public static final String RP_SUFFIX = ":suffix";
    public static final String RP_BATCH_SIZE = ":batch_size";
    public static final String RP_THROTTLE = ":throttle";

    public static final Set<String> RESERVED_PARAMS = Collections.unmodifiableSet(
            new HashSet<String>(
                    Arrays.asList(
                            RP_INTERRUPT,
                            RP_REMOTE_HOST,
                            RP_REMOTE_PORT,
                            RP_REMOTE_IS_HTTPS,
                            RP_REMOTE_USER,
                            RP_REMOTE_PASS,
                            RP_REMOTE_CONTEXT_PATH,
                            RP_REMOTE_STRATEGY,
                            RP_SUFFIX,
                            RP_SELECTORS,
                            RP_UPDATE,
                            RP_ONLY_NEWER,
                            RP_BATCH_SIZE,
                            RP_THROTTLE
                    )
            )
    );

}
