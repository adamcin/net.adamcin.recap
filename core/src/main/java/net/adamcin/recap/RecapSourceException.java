package net.adamcin.recap;

/**
 * @author madamcin
 * @version $Id: RecapSourceException.java$
 */
public class RecapSourceException extends RecapException {

    public RecapSourceException() {
    }

    public RecapSourceException(String message) {
        super(message);
    }

    public RecapSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecapSourceException(Throwable cause) {
        super(cause);
    }
}
