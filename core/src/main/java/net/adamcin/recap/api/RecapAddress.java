package net.adamcin.recap.api;

/**
 * @author madamcin
 * @version $Id: RecapAddress.java$
 */
public interface RecapAddress {
    String getHostname();
    Integer getPort();
    boolean isHttps();
    String getUsername();
    String getPassword();
    String getContextPath();
}
