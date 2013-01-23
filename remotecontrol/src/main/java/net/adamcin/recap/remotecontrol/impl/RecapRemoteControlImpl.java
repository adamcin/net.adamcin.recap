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

package net.adamcin.recap.remotecontrol.impl;

import net.adamcin.recap.api.Recap;
import net.adamcin.recap.api.RecapAddress;
import net.adamcin.recap.impl.RecapAddressImpl;
import net.adamcin.recap.remotecontrol.RecapPath;
import net.adamcin.recap.remotecontrol.RecapRemoteControl;
import net.adamcin.recap.remotecontrol.RecapRemoteException;
import net.adamcin.recap.remotecontrol.RecapRequest;
import net.adamcin.recap.remotecontrol.RecapStrategy;
import net.adamcin.recap.remotecontrol.RecapStrategyDescriptor;
import net.adamcin.recap.remotecontrol.RecapUtil;
import net.adamcin.recap.remotecontrol.RemoteControlConstants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author madamcin
 * @version $Id: RecapRemoteControlImpl.java$
 */
@Component
@Service
public class RecapRemoteControlImpl implements RecapRemoteControl {

    @Reference
    private Recap recap;

    @Reference
    private MetaTypeService metaTypeService;

    private RecapStrategyManager strategyManager;

    protected void activate(ComponentContext ctx) {
        strategyManager = new RecapStrategyManager(ctx.getBundleContext(), metaTypeService);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        strategyManager = null;
    }

    private InputStreamReader executeRequest(RecapAddress address, String getPath) throws IOException {

        RecapAddress addr = applyAddressDefaults(address);

        String uri;
        if (StringUtils.isNotEmpty(addr.getContextPath())) {
            uri = addr.getContextPath() + getPath;
        } else {
            uri = getPath;
        }

        GetMethod method = new GetMethod(uri);

        HttpHost targetHost = new HttpHost(addr.getHostname(),
                addr.getPort(), addr.isHttps() ? Protocol.getProtocol("https") : Protocol.getProtocol("http"));

        HttpClient client = new HttpClient();

        client.getHostConfiguration().setHost(addr.getHostname(), addr.getPort());
        client.getParams().setAuthenticationPreemptive(true);
        client.getState().setCredentials(
                new AuthScope(addr.getHostname(), addr.getPort()),
                new UsernamePasswordCredentials(addr.getUsername(), addr.getPassword())
        );

        int result = client.executeMethod(method);
        if (result == 200) {
            return new InputStreamReader(method.getResponseBodyAsStream(), method.getResponseCharSet());
        } else {
            return null;
        }
    }

    public Iterator<RecapPath> listRemotePaths(RecapAddress address, RecapRequest request) throws RecapRemoteException {
        RecapAddress addr = applyAddressDefaults(address);
        RecapRequest req = applyRequestDefaults(request);
        StringBuilder pathBuilder = new StringBuilder(RemoteControlConstants.SERVLET_LIST_PATH);
        for (String selector : req.getSelectors()) {
            pathBuilder.append(".").append(selector);
        }

        pathBuilder.append(".txt");
        if (StringUtils.isNotEmpty(req.getSuffix())) {
            if (!req.getSuffix().startsWith("/")) {
                pathBuilder.append("/");
            }
            pathBuilder.append(req.getSuffix());
        }

        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new NameValuePair(RemoteControlConstants.RP_STRATEGY, req.getStrategy()));

        if (req.getParameters() != null) {
            pairs.addAll(req.getParameters());
        }

        pathBuilder.append("?").append(RecapUtil.format(pairs, RecapUtil.UTF_8));

        InputStreamReader reader = null;

        try {
            reader = executeRequest(addr, pathBuilder.toString());
            List<RecapPath> paths = new ArrayList<RecapPath>();

            if (reader != null) {

                BufferedReader breader = new BufferedReader(reader);

                String line;
                while ((line = breader.readLine()) != null) {
                    RecapPath path = RecapPath.parse(line);
                    if (path != null) {
                        paths.add(path);
                    }
                }
            }

            return paths.iterator();
        } catch (IOException e) {
            throw new RecapRemoteException("Failed to list remote paths.", e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public List<RecapStrategyDescriptor> listRemoteStrategies(RecapAddress context) throws RecapRemoteException {
        List<RecapStrategyDescriptor> strategies = new ArrayList<RecapStrategyDescriptor>();
        try {
            InputStreamReader reader = executeRequest(context, RemoteControlConstants.SERVLET_STRATEGIES_PATH);
            if (reader != null) {
                JSONArray array = CDL.toJSONArray(new JSONTokener(reader));

                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject jo = array.optJSONObject(i);
                        if (jo != null) {
                            String type = jo.optString(RemoteControlConstants.KEY_STRATEGY_TYPE);
                            if (type != null) {
                                RecapStrategyDescriptorImpl descriptor = new RecapStrategyDescriptorImpl();
                                descriptor.setType(type);
                                descriptor.setLabel(jo.optString(RemoteControlConstants.KEY_STRATEGY_LABEL));
                                descriptor.setDescription(jo.optString(RemoteControlConstants.KEY_STRATEGY_DESCRIPTION));
                                strategies.add(descriptor);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw new RecapRemoteException(e);
        } catch (IOException e) {
            throw new RecapRemoteException(e);
        }
        return strategies;
    }

    public List<RecapStrategyDescriptor> listLocalStrategies() {
        return this.strategyManager.listStrategyDescriptors();
    }

    public RecapStrategy getStrategy(String strategyType) {
        if (StringUtils.isNotEmpty(strategyType)) {
            return this.strategyManager.newInstance(strategyType);
        }
        return null;
    }

    public void ungetStrategy(RecapStrategy strategy) {
        this.strategyManager.release(strategy);
    }

    private RecapAddress applyAddressDefaults(final RecapAddress address) {
        RecapAddressImpl dAddress= new RecapAddressImpl();

        dAddress.setPort(recap.getDefaultPort());
        dAddress.setUsername(recap.getDefaultUsername());
        dAddress.setPassword(recap.getDefaultPassword());
        dAddress.setContextPath(recap.getDefaultContextPath());

        if (address != null) {

            dAddress.setHostname(address.getHostname());
            dAddress.setHttps(address.isHttps());

            if (address.getPort() != null) {
                dAddress.setPort(address.getPort());
            }
            if (address.getUsername() != null) {
                dAddress.setUsername(address.getUsername());
            }
            if (address.getPassword() != null) {
                dAddress.setPassword(address.getPassword());
            }
            if (address.getContextPath() != null) {
                dAddress.setContextPath(address.getContextPath());
            }
        }

        return dAddress;
    }

    private RecapRequest applyRequestDefaults(final RecapRequest request) {
        RecapRequestImpl dRequest= new RecapRequestImpl();

        dRequest.setStrategy(RemoteControlConstants.DIRECT_STRATEGY);
        dRequest.setSelectors(Collections.<String>emptyList());
        dRequest.setSuffix("");
        dRequest.setParameters(Collections.<NameValuePair>emptyList());

        if (request != null) {
            if (request.getStrategy() != null) {
                dRequest.setStrategy(request.getStrategy());
            }
            if (request.getSelectors() != null) {
                dRequest.setSelectors(request.getSelectors());
            }
            if (request.getSuffix() != null) {
                dRequest.setSuffix(request.getSuffix());
            }
            if (request.getParameters() != null) {
                dRequest.setParameters(request.getParameters());
            }
        }

        return dRequest;
    }

}
