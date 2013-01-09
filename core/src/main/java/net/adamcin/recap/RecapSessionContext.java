package net.adamcin.recap;


import org.apache.commons.httpclient.NameValuePair;

import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapSessionContext.java$
 */
public interface RecapSessionContext {
    RecapSourceContext  getSourceContext();
    String getStrategy();
    String[] getSelectors();
    String getSuffix();
    List<NameValuePair> getParameters();
}
