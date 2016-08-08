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
import com.day.cq.replication.AgentConfigGroup;
import org.apache.sling.api.resource.ValueMap;

public class MockTransportAgentConfig implements AgentConfig {

    String uri;
    String username;
    String password;

    public MockTransportAgentConfig(String uri, String username, String password) {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }

    public String getTransportURI() {
        return uri;
    }

    public String getTransportUser() {
        return username;
    }

    public String getTransportPassword() {
        return password;
    }

    public void checkValid() {
        throw new UnsupportedOperationException("checkValid not implemented");
    }

    public String getId() {
        throw new UnsupportedOperationException("getId not implemented");
    }

    public String getAgentId() {
        throw new UnsupportedOperationException("getAgentId not implemented");
    }

    public String getConfigPath() {
        throw new UnsupportedOperationException("getConfigPath not implemented");
    }

    public String getName() {
        throw new UnsupportedOperationException("getName not implemented");
    }

    public AgentConfigGroup getConfigGroup() {
        throw new UnsupportedOperationException("getConfigGroup not implemented");
    }

    public String getSerializationType() {
        throw new UnsupportedOperationException("getSerializationType not implemented");
    }

    public String getAgentUserId() {
        throw new UnsupportedOperationException("getAgentUserId not implemented");
    }

    public long getRetryDelay() {
        throw new UnsupportedOperationException("getRetryDelay not implemented");
    }

    public boolean isEnabled() {
        throw new UnsupportedOperationException("isEnabled not implemented");
    }

    public String getLogLevel() {
        throw new UnsupportedOperationException("getLogLevel not implemented");
    }

    public int getMaxRetries() {
        throw new UnsupportedOperationException("getMaxRetries not implemented");
    }

    public boolean isSpecific() {
        throw new UnsupportedOperationException("isSpecific not implemented");
    }

    public boolean isTriggeredOnModification() {
        throw new UnsupportedOperationException("isTriggeredOnModification not implemented");
    }

    public boolean isTriggeredOnOffTime() {
        throw new UnsupportedOperationException("isTriggeredOnOffTime not implemented");
    }

    public boolean isTriggeredOnDistribute() {
        throw new UnsupportedOperationException("isTriggeredOnDistribute not implemented");
    }

    public boolean usedForReverseReplication() {
        throw new UnsupportedOperationException("usedForReverseReplication not implemented");
    }

    public boolean noVersions() {
        throw new UnsupportedOperationException("noVersions not implemented");
    }

    public boolean noStatusUpdate() {
        throw new UnsupportedOperationException("noStatusUpdate not implemented");
    }

    public ValueMap getProperties() {
        throw new UnsupportedOperationException("getProperties not implemented");
    }

    @Override
    public String[] getAllTransportURIs() {
        return new String[]{ getTransportURI() };
    }

    @Override
    public boolean isBatchMode() {
        return false;
    }

    @Override
    public long getBatchWaitTime() {
        return 0;
    }

    @Override
    public long getBatchMaxSize() {
        return 0;
    }

    @Override
    public boolean isTriggeredOnReceive() {
        return false;
    }

    @Override
    public boolean isInMaintenanceMode() {
        return false;
    }

    @Override
    public boolean aliasUpdate() {
        return false;
    }

    @Override
    public String getSSLConfig() {
        return null;
    }

    @Override
    public boolean allowsExpiredCertificates() {
        return false;
    }

    @Override
    public boolean isOAuthEnabled() {
        return false;
    }
}
