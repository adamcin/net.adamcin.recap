package net.adamcin.recap;

/**
 * @author madamcin
 * @version $Id: RecapStrategyException.java$
 */
public class RecapStrategyException extends Exception {

    public RecapStrategyException() {
    }

    public RecapStrategyException(String message) {
        super(message);
    }

    public RecapStrategyException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecapStrategyException(Throwable cause) {
        super(cause);
    }
}
