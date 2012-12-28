package net.adamcin.recap;

import org.apache.http.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRemoteContext.java$
 */
public interface RecapRemoteContext {
    String getRemoteHost();
    int getRemotePort();
    String getRemoteUsername();
    String getRemotePassword();

    String getStrategy();
    String[] getSelectors();
    String getSuffix();
    List<NameValuePair> getParameters();
}
