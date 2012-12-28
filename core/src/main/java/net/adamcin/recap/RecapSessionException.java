package net.adamcin.recap;

/**
 * @author madamcin
 * @version $Id: RecapSessionException.java$
 */
public class RecapSessionException extends Exception {

    public RecapSessionException() {
    }

    public RecapSessionException(String message) {
        super(message);
    }

    public RecapSessionException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecapSessionException(Throwable cause) {
        super(cause);
    }
}
