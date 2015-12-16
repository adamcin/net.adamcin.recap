/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.recap.api;

/**
 * Class hosting several important Recap API constants
 * @author madamcin
 * @version $Id: RecapConstants.java$
 */
public final class RecapConstants {

    private RecapConstants() {
        // prevent instantiation
    }

    /**
     * Path to the basic Recap copy servlet
     */
    public static final String SERVLET_COPY_PATH = "/bin/recap/copy";

    // ------------------------------------------------
    // Default Default Config Values
    // ------------------------------------------------
    public static final int DEFAULT_DEFAULT_PORT = 8080;
    public static final String DEFAULT_DEFAULT_SERVLET_PATH = "/server";
    public static final String DEFAULT_DEFAULT_USERNAME = "anonymous";
    public static final String DEFAULT_DEFAULT_PASSWORD = "anonymous";
    public static final int DEFAULT_DEFAULT_BATCH_SIZE = 1024;
    public static final String DEFAULT_DEFAULT_REQUEST_DEPTH_CONFIG = "";
    public static final String DEFAULT_DEFAULT_LAST_MODIFIED_PROPERTY = "";

    // ------------------------------------------------
    // Request Parameters related to RecapAddresses
    // ------------------------------------------------
    public static final String RP_HOSTNAME = ":host";
    public static final String RP_PORT = ":port";
    public static final String RP_IS_HTTPS = ":is_https";
    public static final String RP_USERNAME = ":user";
    public static final String RP_PASSWORD = ":pass";
    public static final String RP_SERVLET_PATH = ":servlet_path";

    // ------------------------------------------------
    // Request Parameters related to RecapOptions
    // ------------------------------------------------
    public static final String RP_UPDATE = ":update";
    public static final String RP_ONLY_NEWER = ":only_newer";
    public static final String RP_KEEP_ORDER = ":keep_order";
    public static final String RP_REVERSE = ":reverse";
    public static final String RP_NO_RECURSE = ":no_recurse";
    public static final String RP_NO_DELETE = ":no_delete";
    public static final String RP_BATCH_SIZE = ":batch_size";
    public static final String RP_REQUEST_DEPTH_CONFIG = ":request_depth_config";
    public static final String RP_THROTTLE = ":throttle";
    public static final String RP_LAST_MODIFIED_PROPERTY = ":last_modified_property";

}
