package net.adamcin.recap.remotecontrol;


import org.apache.commons.httpclient.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRequest.java$
 */
public interface RecapRequest {
    String getStrategy();
    List<String> getSelectors();
    String getSuffix();
    List<NameValuePair> getParameters();
}
