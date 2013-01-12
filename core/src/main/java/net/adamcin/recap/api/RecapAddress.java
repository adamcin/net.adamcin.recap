package net.adamcin.recap.api;

/**
 * @author madamcin
 * @version $Id: RecapAddress.java$
 */
public interface RecapAddress {
    String getHostname();
    int getPort();
    boolean isHttps();
    String getUsername();
    String getPassword();
    String getContextPath();
}
