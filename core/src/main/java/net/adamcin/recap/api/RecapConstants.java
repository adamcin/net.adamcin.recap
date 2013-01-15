package net.adamcin.recap.api;

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
     * Path to the basic Recap copy servlet
     */
    public static final String SERVLET_COPY_PATH = "/bin/recap/copy";

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
    // Request Parameters related to RecapOptions
    // ------------------------------------------------
    public static final String RP_UPDATE = ":update";
    public static final String RP_ONLY_NEWER = ":only_newer";
    public static final String RP_BATCH_SIZE = ":batch_size";
    public static final String RP_THROTTLE = ":throttle";
    public static final String RP_LAST_MODIFIED_PROPERTY = ":last_modified_property";

}
