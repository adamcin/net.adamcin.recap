package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapAddress;

/**
 * @author madamcin
 * @version $Id: RecapRequestImpl.java$
 */
public class RecapAddressImpl implements RecapAddress {

    String hostname;
    Integer port;
    boolean https;
    String username;
    String password;
    String contextPath;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }
}
