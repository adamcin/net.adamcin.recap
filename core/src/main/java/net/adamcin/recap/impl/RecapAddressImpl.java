package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapAddress;

/**
 * @author madamcin
 * @version $Id: RecapRequestImpl.java$
 */
public class RecapAddressImpl implements RecapAddress {

    String remoteHost;
    int remotePort;
    boolean https;
    String remoteUsername;
    String remotePassword;
    String contextPath;

    public String getHostname() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getPort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public String getUsername() {
        return remoteUsername;
    }

    public void setRemoteUsername(String remoteUsername) {
        this.remoteUsername = remoteUsername;
    }

    public String getPassword() {
        return remotePassword;
    }

    public void setRemotePassword(String remotePassword) {
        this.remotePassword = remotePassword;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
