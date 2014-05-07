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
 * Object representing connection details for a remote JCR repository
 * DavEx endpoint. A URL constructed from this address would follow
 * the pattern:
 *
 * http(s)://[username]:[password]@[hostname]:[port][contextPath][prefix]
 *
 * @author madamcin
 * @version $Id: RecapAddress.java$
 */
public interface RecapAddress {

    /**
     * @return whether to use HTTPS (SSL) instead of HTTP
     */
    boolean isHttps();

    /**
     * @return the hostname of the address
     */
    String getHostname();

    /**
     * @return the TCP port of the address
     */
    Integer getPort();

    /**
     * @return the username for the remote session
     */
    String getUsername();

    /**
     * @return the password for the remote session
     */
    String getPassword();

    /**
     * @since 1.0
     * @return the remote DavEx Servlet Path
     */
    String getServletPath();
}
