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

package net.adamcin.recap.batchsession;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class DefaultBatchSessionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBatchSessionTest.class);

    TransientRepository repository;

    @Before
    public void setUp() throws Exception {
        repository = new TransientRepository(new File(new File("target"),
                DefaultBatchSessionTest.class.getName()));
    }

    @After
    public void tearDown() throws Exception {
        repository.shutdown();
    }

    @Test
    public void testCreateSession() {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

            BatchSaveTracker tracker = new BatchSaveTracker();

            DefaultBatchSession batchSession = new DefaultBatchSession(session);
            batchSession.setBatchSize(5);
            batchSession.addListener(tracker);

            assertTrue("managed session is live", batchSession.isLive());

            session.logout();

            assertFalse("managed session is not live", batchSession.isLive());


        } catch (Exception e) {
            LOGGER.error("Exception: {}", e);
            TestUtil.sprintFail(e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    @Test
    public void testAddNodes() {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

            BatchSaveTracker tracker = new BatchSaveTracker();

            DefaultBatchSession batchSession = new DefaultBatchSession(session);
            batchSession.setBatchSize(5);
            batchSession.addListener(tracker);

            Node n0 = batchSession.getRootNode().addNode("n0", JcrConstants.NT_FOLDER);

            assertEquals("managed session should be returned from Node.getSession()",
                    batchSession, n0.getSession());

            Node n1 = n0.addNode("n1", JcrConstants.NT_FOLDER);
            Node n2 = n1.addNode("n2", JcrConstants.NT_FOLDER);
            assertEquals("no changes yet", 0, tracker.getChanges());
            assertTrue("original session should have pending changes",
                    session.hasPendingChanges());
            Node n3 = n2.addNode("n3", JcrConstants.NT_FOLDER);
            assertEquals("should have 5 changes", 5, tracker.getChanges());
            assertFalse("original session should not have pending changes",
                    session.hasPendingChanges());


            Node n4 = n3.addNode("n4", JcrConstants.NT_FOLDER);
            assertEquals("still has 5 changes (after n4)", 5, tracker.getChanges());

            // force save
            n4.getSession().save();
            assertEquals("should have 2 more changes (after n4)", 7, tracker.getChanges());

        } catch (Exception e) {
            LOGGER.error("Exception: {}", e);
            TestUtil.sprintFail(e);
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    static class BatchSaveTracker extends DefaultBatchSessionListener {
        int saves = 0;
        int changes = 0;
        Set<String> paths = new HashSet<String>();

        @Override
        public void onSave(BatchSaveInfo info) {
            super.onSave(info);
            int savedChanges = info.getSavedChanges();
            Set<String> changedPaths = info.getChangedPaths();
            LOGGER.info("[onSave] savedChanges={}, paths={}", savedChanges, changedPaths);
            saves++;
            changes += savedChanges;
            paths.addAll(changedPaths);
        }

        public int getSaves() {
            return saves;
        }

        public int getChanges() {
            return changes;
        }

        public Set<String> getPaths() {
            return paths;
        }
    }
}
