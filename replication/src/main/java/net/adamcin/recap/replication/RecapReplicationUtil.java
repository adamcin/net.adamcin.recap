/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.recap.replication;

import com.day.cq.replication.AgentConfig;
import com.day.cq.replication.ReplicationException;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.util.DefaultRecapAddress;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Useful constants and static methods for API-level usage of Recap replication agents
 * in Adobe Granite
 */
public final class RecapReplicationUtil {

    /**
     * specify recap replication agent transport URI using recap+http:// or recap+https:// scheme
     */
    public static final String TRANSPORT_URI_SCHEME_PREFIX = "recap+";

    /**
     * specify recap replication agent serialization type to "recap"
     */
    public static final String SERIALIZATION_TYPE = "recap";

    private RecapReplicationUtil() {
        // prevent instantiation
    }

    /**
     * Builds a RecapAddress from related properties of the provided AgentConfig
     * @param config
     * @return
     * @throws com.day.cq.replication.ReplicationException
     */
    public static RecapAddress getAgentAddress(AgentConfig config) throws ReplicationException {

        final String uri = config.getTransportURI();

        if (!uri.startsWith(TRANSPORT_URI_SCHEME_PREFIX)) {
            throw new ReplicationException("uri must start with " + TRANSPORT_URI_SCHEME_PREFIX);
        }

        final String user = config.getTransportUser();
        final String pass = config.getTransportPassword();

        try {

            final URI parsed = new URI(uri.substring(TRANSPORT_URI_SCHEME_PREFIX.length()));
            final boolean https = "https".equals(parsed.getScheme());
            final String host = parsed.getHost();
            final int port = parsed.getPort() < 0 ? (https ? 443 : 80) : parsed.getPort();
            final String prefix = StringUtils.isEmpty(parsed.getPath()) ? null : parsed.getPath();

            return new DefaultRecapAddress() {
                @Override public boolean isHttps() { return https; }
                @Override public String getHostname() { return host; }
                @Override public Integer getPort() { return port; }
                @Override public String getUsername() { return user; }
                @Override public String getPassword() { return pass; }
                @Override public String getServletPath() { return prefix; }
            };
        } catch (URISyntaxException e) {
            throw new ReplicationException(e);
        }
    }
}
