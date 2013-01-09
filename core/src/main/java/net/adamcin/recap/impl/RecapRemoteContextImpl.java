package net.adamcin.recap.impl;

import net.adamcin.recap.RecapRemoteContext;
import org.apache.commons.httpclient.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRemoteContextImpl.java$
 */
public class RecapRemoteContextImpl implements RecapRemoteContext {

    String remoteHost;
    int remotePort;
    boolean https;
    String remoteUsername;
    String remotePassword;
    String contextPath;
    String strategy;
    String[] selectors;
    String suffix;
    List<NameValuePair> parameters;

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort() {
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

    public String getRemoteUsername() {
        return remoteUsername;
    }

    public void setRemoteUsername(String remoteUsername) {
        this.remoteUsername = remoteUsername;
    }

    public String getRemotePassword() {
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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String[] getSelectors() {
        return selectors;
    }

    public void setSelectors(String[] selectors) {
        this.selectors = selectors;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public List<NameValuePair> getParameters() {
        return parameters;
    }

    public void setParameters(List<NameValuePair> parameters) {
        this.parameters = parameters;
    }
}
