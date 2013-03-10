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
 * Object representing a remote DavEx repository connection for
 * synchronizing paths with a local JCR session.
 *
 * @author madamcin
 * @version $Id: RecapSession.java$
 */
public interface RecapSession {

    /**
     * Checks for appropriate permissions on source and target repositories
     * at the specified path, without executing a sync
     * @param path
     * @throws RecapSessionException if permissions are insufficient
     */
    void checkPermissions(String path) throws RecapSessionException;

    /**
     * Synchronize a single root path to the target repository:
     *  1.  create any missing ancestor nodes as stubs in the
     *      target repository.
     *  2.  create a node at the specified path if it doesn't exist
     *  3.  copy properties from source node to the target node
     *      according to provided options
     *  4.  (if recursive) copy descendants according to provided
     *      options
     * @param path the path to sync
     * @throws RecapSessionException
     */
    void sync(String path) throws RecapSessionException;

    /**
     * Synchronize a single root path to the target repository:
     *  1.  create any missing ancestor nodes as stubs in the
     *      target repository.
     *  2.  create a node at the specified path if it doesn't exist
     *  3.  copy properties from source node to the target node
     *      according to provided options
     *  4.  If source node has a jcr:content child node, sync the
     *      jcr:content node to the target repository, obeying
     *      recursive option for descendants of the jcr:content node
     * @param path
     * @throws RecapSessionException
     */
    void syncContent(String path) throws RecapSessionException;

    void delete(String path) throws RecapSessionException;

    /**
     * Commit any remaining changes to the target repository and finalize
     * session statistics
     * @throws RecapSessionException if commit fails
     */
    void finish() throws RecapSessionException;

    /**
     * Logout the remote DavEx repository Session
     */
    void logout();

    /**
     * Get the RecapOptions provided at the creation of the session,
     * incorporating default values provided by the Recap service.
     * @return
     */
    RecapOptions getOptions();

    /**
     * Provide a progress listener for the session
     * @param progressListener
     */
    void setProgressListener(RecapProgressListener progressListener);

    RecapProgressListener getProgressListener();

    Session getLocalSession();

    //-----------------------------------------------------------------
    // Getters for Session Statistics
    //-----------------------------------------------------------------
    boolean isFinished();

    int getTotalSyncPaths();

    String getLastSuccessfulSyncPath();

    int getTotalNodes();

    long getTotalSize();

    long getTotalTimeMillis();
}
