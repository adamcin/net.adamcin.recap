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
        print("recap-error", "E", String.format("-------- %s (%s)", path, ex.getMessage()));
    }

    public void onFailure(String path, Exception ex) {
        print("recap-error", "F", String.format("-------- %s (%s)", path, ex.getMessage()));
    }

    public void onPath(PathAction action, int count, String path) {
        print("recap-path", action.toString(), String.format("%08d %s", count, path));
    }

    protected void print(String cssClass, String action, String line) {
        writer.printf("<span class='%s'><strong>%s</strong>&nbsp;%s</span>%n", cssClass, action, line);
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
