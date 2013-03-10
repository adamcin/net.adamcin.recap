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
import net.adamcin.recap.api.RecapProgressListener;

/**
 * Implementation of RecapProgressListener that wraps a ReplicationLog.
 */
public class ReplicationLogProgressListener implements RecapProgressListener {

    private final ReplicationLog log;

    public ReplicationLogProgressListener(ReplicationLog log) {
        this.log = log;
    }

    public void onMessage(String fmt, Object... args) {
        if (atLeast(log.getLevel(), ReplicationLog.Level.INFO)) {
            log.info("M %s", String.format(fmt, args));
        }
    }

    public void onError(String path, Exception ex) {
        if (atLeast(log.getLevel(), ReplicationLog.Level.WARN)) {
            log.warn("E %s (%s)", path, ex.getMessage());
        }
    }

    public void onFailure(String path, Exception ex) {
        if (atLeast(log.getLevel(), ReplicationLog.Level.ERROR)) {
            log.warn("F %s (%s)", path, ex.getMessage());
        }
    }

    public void onPath(PathAction action, int count, String path) {
        if (atLeast(log.getLevel(), ReplicationLog.Level.DEBUG)) {
            log.warn("%s %s", action, path);
        }
    }

    public static boolean atLeast(ReplicationLog.Level logLevel, ReplicationLog.Level compareToLevel) {
        return logLevel.compareTo(compareToLevel) <= 0;
    }
}
