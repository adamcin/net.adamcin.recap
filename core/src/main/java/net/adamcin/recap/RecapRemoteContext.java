package net.adamcin.recap;


import org.apache.commons.httpclient.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRemoteContext.java$
 */
public interface RecapRemoteContext {
    String getRemoteHost();
    int getRemotePort();
    boolean isHttps();
    String getRemoteUsername();
    String getRemotePassword();

    String getContextPath();
    String getStrategy();
    String[] getSelectors();
    String getSuffix();
    List<NameValuePair> getParameters();
}
