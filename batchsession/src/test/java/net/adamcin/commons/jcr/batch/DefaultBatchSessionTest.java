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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

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
            assertEquals("no changes yet", 0, tracker.getTotalCount());
            assertTrue("original session should have pending changes",
                    session.hasPendingChanges());
            Node n3 = n2.addNode("n3", JcrConstants.NT_FOLDER);
            assertEquals("should have 5 changes", 5, tracker.getTotalCount());
            assertFalse("original session should not have pending changes",
                    session.hasPendingChanges());


            Node n4 = n3.addNode("n4", JcrConstants.NT_FOLDER);
            assertEquals("still has 5 changes (after n4)", 5, tracker.getTotalCount());

            // force save
            n4.getSession().save();
            assertEquals("should have 2 more changes (after n4)", 7, tracker.getTotalCount());


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
    public void testPurgeVersion() {
        Session session = null;
        try {
            session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

            BatchSaveTracker tracker = new BatchSaveTracker();

            DefaultBatchSession batchSession = new DefaultBatchSession(session);
            batchSession.setBatchSize(5);
            batchSession.addListener(tracker);

            VersionManager vm = batchSession.getWorkspace().getVersionManager();

            Node v0 = batchSession.getRootNode().addNode("v0", JcrConstants.NT_UNSTRUCTURED);

            v0.addMixin(JcrConstants.MIX_VERSIONABLE);
            v0.setProperty("versionState", "initial");

            assertEquals("versionState property should be", "initial", v0.getProperty("versionState").getString());

            batchSession.save();

            vm.checkin(v0.getPath());
            vm.checkout(v0.getPath());

            VersionHistory vh0 = vm.getVersionHistory(v0.getPath());

            String vhPath = vh0.getPath();

            Node v0r0 = vh0.getVersion("1.0").getFrozenNode();

            assertTrue("version 1.0 frozen node should have versionState property", v0r0.hasProperty("versionState"));
            assertEquals("versionState property should be", "initial", v0r0.getProperty("versionState").getString());

            batchSession.removeItem(v0.getPath());

            batchSession.save();

            assertFalse("/v0 should no longer exist", batchSession.getRootNode().hasNode("v0"));
            assertEquals("tracker should indicate no total purged versions", 0, tracker.getTotalVersionCount());
            assertEquals("tracker should indicate no expected purged versions", 0, tracker.getExpectedVersionCount());

            assertTrue("there should still be a version history at the same path as before",
                    batchSession.getRootNode().hasNode(vhPath.substring(1)));

            Node v1 = batchSession.getRootNode().addNode("v1", JcrConstants.NT_UNSTRUCTURED);

            v1.addMixin(JcrConstants.MIX_VERSIONABLE);
            v1.setProperty("versionState", "initial");

            assertEquals("versionState property should be", "initial", v1.getProperty("versionState").getString());

            batchSession.save();

            vm.checkin(v1.getPath());
            vm.checkout(v1.getPath());

            VersionHistory vh1 = vm.getVersionHistory(v1.getPath());

            String vh1Path = vh1.getPath();

            Node v1r0 = vh1.getVersion("1.0").getFrozenNode();

            assertTrue("version 1.0 frozen node should have versionState property", v1r0.hasProperty("versionState"));
            assertEquals("versionState property should be", "initial", v1r0.getProperty("versionState").getString());

            batchSession.purge(v1.getPath());

            assertEquals("tracker should indicate zero total purged versions", 0, tracker.getTotalVersionCount());
            assertEquals("tracker should indicate one expected purged versions", 1, tracker.getExpectedVersionCount());

            batchSession.save();

            assertFalse("/v1 should no longer exist", batchSession.getRootNode().hasNode("v1"));
            assertEquals("tracker should indicate one total purged versions", 1, tracker.getTotalVersionCount());
            assertEquals("tracker should indicate one expected purged versions", 1, tracker.getExpectedVersionCount());


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
        int totalCount = 0;
        Set<String> totalPaths = new HashSet<String>();
        int totalVersionCount = 0;
        int expectedVersionCount = 0;

        @Override
        public void onSave(BatchSaveInfo info) {
            super.onSave(info);
            int count = info.getCount();
            Set<String> paths = info.getPaths();
            long time = info.getTime();
            LOGGER.info("[onSave] count={}, paths={}, time={}",
                    new Object[]{count, paths, time});
            saves++;
            totalCount += count;
            totalVersionCount += info.getPurgedVersionCount();
            totalPaths.addAll(paths);
        }

        @Override
        public void onRemove(BatchRemoveInfo info) {
            super.onRemove(info);
            expectedVersionCount += info.getPurgedVersionCount();
        }

        public int getSaves() {
            return saves;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public Set<String> getTotalPaths() {
            return totalPaths;
        }

        public int getTotalVersionCount() {
            return totalVersionCount;
        }

        public int getExpectedVersionCount() {
            return expectedVersionCount;
        }
    }
}
