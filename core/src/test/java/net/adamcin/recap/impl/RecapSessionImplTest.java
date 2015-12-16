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

package net.adamcin.recap.impl;


import net.adamcin.commons.testing.junit.TestBody;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapProgressListener;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.io.File;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class RecapSessionImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecapSessionImplTest.class);

    private static final Credentials DEFAULT_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    private static final RecapAddress DEFAULT_ADDRESS = new RecapAddress() {
        public boolean isHttps() { return false; }
        public String getHostname() { return "localhost"; }
        public Integer getPort() { return 4502; }
        public String getUsername() { return "admin"; }
        public String getPassword() { return "admin"; }
        public String getServletPath() { return null; }
    };

    private TransientRepository localRepo;
    private TransientRepository remoteRepo;

    @Before
    public void setUp() throws Exception {
        localRepo = new TransientRepository(new File(new File("target"),
                RecapSessionImplTest.class.getName() + "_local"));

        remoteRepo = new TransientRepository(new File(new File("target"),
                RecapSessionImplTest.class.getName() + "_remote"));
    }

    @After
    public void tearDown() throws Exception {
        localRepo.shutdown();
        remoteRepo.shutdown();
    }

    @Test
    public void testCreateRecapSession() {
        // check that appropriate exceptions are thrown by constructor
        TestBody.test(new RecapTestBody() {
            @Override protected void execute() throws Exception {
                localSession = localRepo.login(DEFAULT_CREDENTIALS);
                remoteSession = remoteRepo.login(DEFAULT_CREDENTIALS);

                boolean npeThrown = false;
                try {
                    new RecapSessionImpl(null, null, null, null, null);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (!npeThrown) {
                        fail("NPE not thrown for interrupter");
                    }
                    npeThrown = false;
                }

                RecapSessionInterrupter interrupter = new TestInterrupter();

                try {
                    new RecapSessionImpl(interrupter, null, null, null, null);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (!npeThrown) {
                        fail("NPE not thrown for address");
                    }
                    npeThrown = false;
                }

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, null, null, null);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (!npeThrown) {
                        fail("NPE not thrown for options");
                    }
                    npeThrown = false;
                }

                RecapOptionsImpl options = new RecapOptionsImpl();

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, null, null);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (!npeThrown) {
                        fail("NPE not thrown for localSession");
                    }
                    npeThrown = false;
                }

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, localSession, null);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (!npeThrown) {
                        fail("NPE not thrown for remoteSession");
                    }
                    npeThrown = false;
                }

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, localSession, remoteSession);
                } catch (NullPointerException e) {
                    npeThrown = true;
                } finally {
                    if (npeThrown) {
                        fail("Unexpected NPE thrown");
                    }
                }

                boolean iaeThrown = false;

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, localSession, localSession);
                } catch (IllegalArgumentException e) {
                    iaeThrown = true;
                } finally {
                    if (!iaeThrown) {
                        fail("IAE not thrown for same session");
                    }
                    iaeThrown = false;
                }

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, remoteSession, remoteSession);
                } catch (IllegalArgumentException e) {
                    iaeThrown = true;
                } finally {
                    if (!iaeThrown) {
                        fail("IAE not thrown for same session");
                    }
                    iaeThrown = false;
                }

                try {
                    new RecapSessionImpl(interrupter, DEFAULT_ADDRESS, options, localSession, remoteSession);
                } catch (IllegalArgumentException e) {
                    iaeThrown = true;
                } finally {
                    if (iaeThrown) {
                        fail("IAE thrown for different sessions");
                    }
                }
            }
        });
    }

    @Test
    public void testBasicSync() {
        TestBody.test(new RecapTestBody() {
            @Override protected void execute() throws Exception {
                localSession = localRepo.login(DEFAULT_CREDENTIALS);
                remoteSession = remoteRepo.login(DEFAULT_CREDENTIALS);

                Node lastNode = remoteSession.getRootNode();
                for (int i = 0; i < 10; i++) {
                    lastNode = lastNode.addNode("basicSync" + i, JcrConstants.NT_UNSTRUCTURED);
                }
                lastNode.getSession().save();

                TestProgressTracker tracker = new TestProgressTracker();

                RecapSessionInterrupter interrupter = new TestInterrupter();
                RecapOptionsImpl options = new RecapOptionsImpl();
                RecapSessionImpl session = new RecapSessionImpl(interrupter,
                        DEFAULT_ADDRESS,
                        options,
                        localSession,
                        remoteSession);

                session.setProgressListener(tracker);

                String lastPath = "";
                int totalNodes = 0;
                for (int i = 0; i < 10; i++) {
                    lastPath = lastPath + "/basicSync" + i;
                    totalNodes += (10 - i);
                    session.sync(lastPath);
                    assertEquals("number of synced paths should be", i + 1, session.getTotalSyncPaths());
                    assertEquals("total number of nodes should be", totalNodes, session.getTotalNodes());
                    assertEquals("last synced path should be", lastPath, session.getLastSuccessfulSyncPath());
                }
            }
        });
    }

    @Test
    public void testBasicDelete() {
        TestBody.test(new RecapTestBody() {
            @Override protected void execute() throws Exception {
                localSession = localRepo.login(DEFAULT_CREDENTIALS);
                remoteSession = remoteRepo.login(DEFAULT_CREDENTIALS);

                Node lastNode = remoteSession.getRootNode();
                for (int i = 0; i < 10; i++) {
                    lastNode = lastNode.addNode("basicDelete" + i, JcrConstants.NT_UNSTRUCTURED);
                }
                lastNode.getSession().save();

                TestProgressTracker tracker = new TestProgressTracker();

                RecapSessionInterrupter interrupter = new TestInterrupter();
                RecapOptionsImpl options = new RecapOptionsImpl();
                RecapSessionImpl session = new RecapSessionImpl(interrupter,
                        DEFAULT_ADDRESS,
                        options,
                        localSession,
                        remoteSession);

                session.setProgressListener(tracker);

                session.sync("/basicDelete0");

                assertEquals("number of synced paths should be", 1, session.getTotalSyncPaths());
                assertEquals("total number of nodes should be", 10, session.getTotalNodes());
                assertEquals("last synced path should be", "/basicDelete0", session.getLastSuccessfulSyncPath());

                session.delete("/basicDelete0");

                assertEquals("number of synced paths should be", 1, session.getTotalSyncPaths());
                assertEquals("total number of nodes should be", 20, session.getTotalNodes());
                assertEquals("last synced path should be", "/basicDelete0", session.getLastSuccessfulSyncPath());
            }
        });
    }

    @Test
    public void testNodeOrdering() {
        TestBody.test(new RecapTestBody() {
            @Override
            protected void execute() throws Exception {
                final String BASIC_NODE_NAME = "syncOrdered";
                localSession = localRepo.login(DEFAULT_CREDENTIALS);
                remoteSession = remoteRepo.login(DEFAULT_CREDENTIALS);

                Node REMOTE_ROOT = remoteSession.getRootNode().addNode(BASIC_NODE_NAME, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 1, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 2, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 3, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 4, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 5, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.addNode(BASIC_NODE_NAME + 6, JcrConstants.NT_UNSTRUCTURED);
                REMOTE_ROOT.getSession().save();

                Node LOCAL_ROOT = localSession.getRootNode().addNode(BASIC_NODE_NAME, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 1, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 5, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 2, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 3, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 4, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.addNode(BASIC_NODE_NAME + 6, JcrConstants.NT_UNSTRUCTURED);
                LOCAL_ROOT.getSession().save();

                TestProgressTracker tracker = new TestProgressTracker();
                RecapSessionInterrupter interrupter = new TestInterrupter();
                RecapOptionsImpl options = new RecapOptionsImpl();
                options.setOnlyNewer(true);
                options.setKeepOrder(true);
                options.setUpdate(true);
                RecapSessionImpl session = new RecapSessionImpl(interrupter,
                        DEFAULT_ADDRESS,
                        options,
                        localSession,
                        remoteSession);
                session.setProgressListener(tracker);
                session.sync("/" + BASIC_NODE_NAME);

                NodeIterator remoteNi = remoteSession.getNode("/" + BASIC_NODE_NAME).getNodes();
                NodeIterator localNi = localSession.getNode("/" + BASIC_NODE_NAME).getNodes();
                while (remoteNi.hasNext()) {
                    assertTrue(remoteNi.nextNode().getName().equals(localNi.nextNode().getName()));
                }
            }
        });
    }

    static class TestInterrupter implements RecapSessionInterrupter, Runnable {
        private boolean interrupted;
        private final long sleepBeforeInterrupting;

        TestInterrupter() {
            this(30L * 1000L);
        }

        TestInterrupter(long sleepBeforeInterrupting) {
            this.sleepBeforeInterrupting = sleepBeforeInterrupting;
            new Thread(this).start();
        }

        public boolean isInterrupted() {
            return interrupted;
        }

        public void setInterrupted(boolean interrupted) {
            this.interrupted = interrupted;
        }

        public void run() {
            try {
                Thread.sleep(this.sleepBeforeInterrupting);
            } catch (InterruptedException e) {
                // ignored
            }
            this.interrupted = true;
        }
    }

    abstract class RecapTestBody extends TestBody {
        Session localSession;
        Session remoteSession;

        @Override
        protected void cleanUp() {
            if (localSession != null && localSession.isLive()) {
                localSession.logout();
            }
            if (remoteSession != null && remoteSession.isLive()) {
                remoteSession.logout();
            }
        }
    }

    class TestProgressTracker implements RecapProgressListener {

        private int pathCount = 0;
        private String lastErrorPath = null;
        private Exception lastError = null;
        private String lastFailurePath = null;
        private Exception lastFailure = null;

        public void reset() {
            pathCount = 0;
            lastErrorPath = null;
            lastError = null;
            lastFailurePath = null;
            lastFailure = null;
        }

        public int getPathCount() {
            return pathCount;
        }

        public String getLastErrorPath() {
            return lastErrorPath;
        }

        public Exception getLastError() {
            return lastError;
        }

        public String getLastFailurePath() {
            return lastFailurePath;
        }

        public Exception getLastFailure() {
            return lastFailure;
        }

        public void onMessage(String fmt, Object... args) {
            LOGGER.info("[tracker#onMessage] M={}", String.format(fmt, args));
        }

        public void onError(String path, Exception ex) {
            LOGGER.info("[tracker#onError] path={}, ex={}", path, ex);
            this.lastErrorPath = path;
            this.lastError = ex;
        }

        public void onFailure(String path, Exception ex) {
            LOGGER.info("[tracker#onFailure] path={}, ex={}", path, ex);
            this.lastFailurePath = path;
            this.lastFailure = ex;
        }

        public void onPath(PathAction action, int count, String path) {
            LOGGER.info("[tracker#onPath] action={}, count={}, path={}",
                    new Object[]{action, count, path});
            ++pathCount;
        }
    }

}
