package net.adamcin.recap;

/**
 * @author madamcin
 * @version $Id: RecapSourceContext.java$
 */
public interface RecapSourceContext {
    String getRemoteHost();
    int getRemotePort();
    boolean isHttps();
    String getRemoteUsername();
    String getRemotePassword();
    String getContextPath();
}
