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

import javax.jcr.Session;

/**
 * The Recap service serves as the entry point for the sync API.
 *
 * It is responsible for providing configuration for default options
 * and address parameters, and to create RecapSessions for other components
 * to use.
 *
 * @author madamcin
 * @version $Id: Recap.java$
 */
public interface Recap {

    /**
     * Initialize a new RecapSession
     * @param localJcrSession a local JCR Session (such as provided by
     *                        ResourceResolver#adaptTo(Session.class)
     *                        with appropriate write permissions for the
     *                        paths that will be synced
     * @param address connection info for the remote repository
     * @param options sync options for the session
     * @return a new RecapSession ready for sync
     * @throws RecapSessionException if a connection can not be established to the remote repository
     */
    RecapSession initSession(Session localJcrSession, RecapAddress address, RecapOptions options)
            throws RecapSessionException;

    /**
     * Format the address as a displayable URL, incorporating default
     * address parameter values as appropriate
     *
     * @param recapAddress the address to be formatted
     * @return a URL identifying the address
     */
    String getDisplayableUrl(RecapAddress recapAddress);

    // -----------------------------------------
    // expose default recap parameters
    // -----------------------------------------

    /**
     * @return the default address port
     */
    int getDefaultPort();

    /**
     * @return the default DavEx Servlet path
     */
    String getDefaultServletPath();

    /**
     * @return the default address username
     */
    String getDefaultUsername();

    /**
     * @return the default address password
     */
    String getDefaultPassword();

    /**
     * @return the default batch size
     */
    int getDefaultBatchSize();

    /**
     * @return the default request depth config
     */
    String getDefaultRequestDepthConfig();

    /**
     * @return the default lastModified property
     */
    String getDefaultLastModifiedProperty();
}
