package net.adamcin.recap;

/**
 * @author madamcin
 * @version $Id: RecapException.java$
 */
public class RecapException extends Exception {

    public RecapException() {
    }

    public RecapException(String message) {
        super(message);
    }

    public RecapException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecapException(Throwable cause) {
        super(cause);
    }
}
