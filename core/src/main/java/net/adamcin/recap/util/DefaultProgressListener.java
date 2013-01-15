package net.adamcin.recap.util;

import net.adamcin.recap.api.RecapProgressListener;

import java.io.PrintWriter;

/**
 * @author madamcin
 * @version $Id: DefaultProgressListener.java$
 */
public class DefaultProgressListener implements RecapProgressListener {

    private final PrintWriter writer;

    public DefaultProgressListener(PrintWriter writer) {
        this.writer = writer;
    }

    public void onMessage(String fmt, Object... args) {
        writer.printf("M %s%n", String.format(fmt, args));
        writer.flush();
    }

    public void onError(String path, Exception ex) {
        writer.printf("E -------- %s (%s)%n", path, ex.getMessage());
        writer.flush();
    }

    public void onFailure(String path, Exception ex) {
        writer.printf("F -------- %s (%s)%n", path, ex.getMessage());
        writer.flush();
    }

    public void onPath(PathAction action, int count, String path) {
        writer.printf("%s %08d %s%n", action, count, path);
        writer.flush();
    }
}
