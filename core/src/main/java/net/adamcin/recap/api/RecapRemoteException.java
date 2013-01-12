package net.adamcin.recap.api;

/**
 * @author madamcin
 * @version $Id: RecapRemoteException.java$
 */
public class RecapRemoteException extends RecapException {

    public RecapRemoteException() {
    }

    public RecapRemoteException(String message) {
        super(message);
    }

    public RecapRemoteException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecapRemoteException(Throwable cause) {
        super(cause);
    }
}
