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

package net.adamcin.recap.util;

import net.adamcin.recap.api.RecapProgressListener;

import java.io.PrintWriter;

/**
 * @author madamcin
 * @version $Id: HtmlProgressListener.java$
 */
public class HtmlProgressListener implements RecapProgressListener {

    private final PrintWriter writer;
    private boolean noScroll;
    private long scrollDelay = 1000L;

    private long lastScrolled = 0L;

    public HtmlProgressListener(PrintWriter writer) {
        this.writer = writer;
    }

    public long getScrollDelay() {
        return scrollDelay;
    }

    public void setScrollDelay(long scrollDelay) {
        this.scrollDelay = scrollDelay;
    }

    public boolean isNoScroll() {
        return noScroll;
    }

    public void setNoScroll(boolean noScroll) {
        this.noScroll = noScroll;
    }

    public void onMessage(String fmt, Object... args) {
        print("recap-message", "M", String.format(fmt, args));
    }

    public void onError(String path, Exception ex) {
        print("recap-error", "E", String.format("%s (%s)", path, ex.getMessage()));
    }

    public void onFailure(String path, Exception ex) {
        print("recap-error", "F", String.format("%s (%s)", path, ex.getMessage()));
    }

    public void onPath(PathAction action, int count, String path) {
        print("recap-path", action.toString(), String.format("%s", path));
    }

    protected void print(String cssClass, String action, String line) {
        writer.printf("<span class='%s'><strong>%s</strong>&nbsp;%s</span><br/>%n", cssClass, action, line);
        if (!noScroll) {
            long now = System.currentTimeMillis();
            if (now > lastScrolled + scrollDelay) {
                lastScrolled = now;
                writer.printf("<script>%n");
                writer.printf("window.scrollTo(0, 1000000);%n");
                writer.printf("</script>%n");
            }
        }
        writer.flush();
    }
}
