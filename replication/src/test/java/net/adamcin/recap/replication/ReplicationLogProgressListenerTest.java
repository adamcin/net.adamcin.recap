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

package net.adamcin.recap.replication;

import com.day.cq.replication.ReplicationLog;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReplicationLogProgressListenerTest {
    @Test
    public void testReplicationLogProgressListenerAtLeast() {
        assertTrue("DEBUG is at least DEBUG",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.DEBUG, ReplicationLog.Level.DEBUG));

        assertTrue("DEBUG is at least INFO",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.DEBUG, ReplicationLog.Level.INFO));

        assertTrue("DEBUG is at least WARN",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.DEBUG, ReplicationLog.Level.WARN));

        assertTrue("DEBUG is at least ERROR",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.DEBUG, ReplicationLog.Level.ERROR));

        assertFalse("INFO is not at least DEBUG",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.INFO, ReplicationLog.Level.DEBUG));

        assertTrue("INFO is at least INFO",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.INFO, ReplicationLog.Level.INFO));

        assertTrue("INFO is at least WARN",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.INFO, ReplicationLog.Level.WARN));

        assertTrue("INFO is at least ERROR",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.INFO, ReplicationLog.Level.ERROR));

        assertFalse("WARN is not at least DEBUG",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.WARN, ReplicationLog.Level.DEBUG));

        assertFalse("WARN is not at least INFO",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.WARN, ReplicationLog.Level.INFO));

        assertTrue("WARN is at least WARN",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.WARN, ReplicationLog.Level.WARN));

        assertTrue("WARN is at least ERROR",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.WARN, ReplicationLog.Level.ERROR));

        assertFalse("ERROR is not at least DEBUG",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.ERROR, ReplicationLog.Level.DEBUG));

        assertFalse("ERROR is not at least INFO",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.ERROR, ReplicationLog.Level.INFO));

        assertFalse("ERROR is not at least WARN",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.ERROR, ReplicationLog.Level.WARN));

        assertTrue("ERROR is at least ERROR",
                ReplicationLogProgressListener.atLeast(
                        ReplicationLog.Level.ERROR, ReplicationLog.Level.ERROR));

    }
}
