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

package net.adamcin.commons.jcr.batch;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Set;

/**
 * Simple interface extension defining additional methods useful to managing batch JCR operations
 */
public interface BatchSession extends Session {

    /**
     * @return the underlying JCR session
     */
    Session getSession();

    /**
     * Add a {@link BatchSessionListener} to the session, to receive notifications when the session auto-saves
     * @param listener the listener to be added
     */
    void addListener(BatchSessionListener listener);

    /**
     * Removes a {@link BatchSessionListener} from the session.
     * @param listener the listener to be removed.
     */
    void removeListener(BatchSessionListener listener);

    /**
     * The batch size determines when the BatchSession will auto-save. When a change is made using this session proxy,
     * it will check to see if the number of paths with uncommitted changes matches or exceeds the batch size to
     * determine if an auto-save is necessary. The default batch size is 1024 nodes.
     * @param batchSize
     */
    void setBatchSize(int batchSize);

    /**
     * @return the currently configured batch size
     */
    int getBatchSize();

    /**
     * @return the number of saves committed by this session since instantiation
     */
    int getTotalSaves();

    /**
     * @return set of all paths with changes committed by this BatchSession
     */
    Set<String> getCommittedPaths();

    /**
     * @return the set of paths that have uncommitted changes according to this BatchSession
     */
    Set<String> getUncommittedPaths();

    /**
     * Ignore calls to session saves. Useful for suppressing save calls in API's that
     * are agnostic to this interface. Call {@link #enableSessionSave()} to re-enable
     * Session.save()
     * @return true if this call changed the state from enabled to disabled
     */
    boolean disableSessionSave();

    /**
     * Re-enable calls to session saves after a call to {@link #disableSessionSave()}
     * @return true if this call changed the state from disabled to enabled
     */
    boolean enableSessionSave();

    /**
     * @return true if calls to {@link #save()} will actually do anything
     */
    boolean isSessionSaveEnabled();

    /**
     * Disables autoSave even when the batchSize is exceeded. Call {@link #enableAutoSave()}
     * to re-enable autoSaves.
     * @return true if this call changed the state from enabled to disabled
     */
    boolean disableAutoSave();

    /**
     * Re-enables autoSave for future changes after a call to {@link #disableAutoSave()}.
     * @return true if this call changed the state from disabled to enabled
     */
    boolean enableAutoSave();

    /**
     * @return true if auto-save logic will kick in after the configured batch size is reached.
     */
    boolean isAutoSaveEnabled();

    /**
     * Immediately re-enables session saves and autoSaves, and then saves any uncommitted changes.
     * Should be called by client code after all changes have been processed for the given JCR
     * session.
     * @throws javax.jcr.RepositoryException
     */
    void commit() throws RepositoryException;

    /**
     * Recursively removes the subgraph of Nodes and related {@link javax.jcr.version.Version} Nodes,
     * descending from {@code path}, by traversing depth-first to the leaves and removing each node
     * on the way up. Versions are marked for removal, but are only removed after a session save because
     * version modifications are workspace-write operations, not session-write operations.
     * @param path the node at the top of the subgraph
     * @throws javax.jcr.RepositoryException if anything goes wrong
     */
    void purge(String path) throws RepositoryException;
}
