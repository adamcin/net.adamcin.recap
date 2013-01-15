package net.adamcin.recap.api;

/**
 * @author madamcin
 * @version $Id: RecapProgressListener.java$
 */
public interface RecapProgressListener {

    void onMessage(String fmt, Object... args);

    void onError(String path, Exception ex);

    void onFailure(String path, Exception ex);

    void onPath(PathAction action, int count, String path);

    enum PathAction {
        ADD("A"), UPDATE("U"), DELETE("D"), NO_ACTION("-");

        String action;
        PathAction(String action) {
            this.action = action;
        }

        String getAction() {
            return action;
        }

        @Override
        public String toString() {
            return action;
        }
    }
}
