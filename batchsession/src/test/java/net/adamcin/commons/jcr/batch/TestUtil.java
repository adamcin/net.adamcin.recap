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

import org.junit.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

public final class TestUtil {

    public static String sprintFullStack(final Throwable t) {
        if (t == null) {
            return "";
        }

        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public static String sprintShortStack(final Throwable t) {
        if (t == null) {
            return "";
        }

        StringWriter writer = new StringWriter();
        Throwable local = new Throwable();

        StackTraceElement[] localStack = local.getStackTrace();
        StackTraceElement[] tStack = t.getStackTrace();

        int m = tStack.length-1, n = localStack.length-1;
        while (m >= 0 && n >=0 && tStack[m].equals(localStack[n])) {
            m--; n--;
        }

        int framesInCommon = tStack.length - 1 - (m + 1);

        t.setStackTrace(Arrays.copyOf(t.getStackTrace(), m + 1, StackTraceElement[].class));
        t.printStackTrace(new PrintWriter(writer));
        t.setStackTrace(tStack);
        StringBuilder sb = new StringBuilder(writer.toString());
        if (framesInCommon != 0) {
            sb.append("\t... " + framesInCommon + " more");
        }
        return sb.toString();
    }

    public static void sprintFail(Throwable ex) {
        Assert.fail(ex == null ? "Null throwable" : "Throwable: " + sprintShortStack(ex));
    }
}
