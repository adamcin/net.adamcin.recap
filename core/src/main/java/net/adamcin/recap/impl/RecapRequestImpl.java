package net.adamcin.recap.impl;

import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.api.RecapRequest;
import org.apache.commons.httpclient.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRequestImpl.java$
 */
public class RecapRequestImpl implements RecapRequest {

    String strategy;
    String[] selectors;
    String suffix;
    List<NameValuePair> parameters;

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
