/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ExclusiveInputException;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.models.api.requests.InputLaunchRequest;
import org.graylog2.restclient.models.api.responses.BuffersResponse;
import org.graylog2.restclient.models.api.responses.EmptyResponse;
import org.graylog2.restclient.models.api.responses.SystemOverviewResponse;
import org.graylog2.restclient.models.api.responses.cluster.RadioSummaryResponse;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import org.graylog2.restclient.models.api.responses.system.*;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import play.Logger;
import play.mvc.Http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Radio extends ClusterEntity {

    public interface Factory {
        Radio fromSummaryResponse(RadioSummaryResponse r);
    }

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Radio.class);
    private final ApiClient api;

    private final Input.Factory inputFactory;

    private final URI transportAddress;
    private String id;
    private String shortNodeId;

    private NodeJVMStats jvmInfo;
    private SystemOverviewResponse systemInfo;
    private BufferInfo bufferInfo;

    @AssistedInject
    public Radio(ApiClient api, Input.Factory inputFactory, @Assisted RadioSummaryResponse r) {
        this.api = api;
        this.inputFactory = inputFactory;

        transportAddress = normalizeUriPath(r.transportAddress);
        id = r.id;
        shortNodeId = r.shortNodeId;
    }

    public synchronized void loadSystemInformation() {
        try {
            systemInfo = api.get(SystemOverviewResponse.class)
                    .path("/system")
                    .radio(this)
                    .execute();
        } catch (APIException e) {
            log.error("Unable to load system information for radio " + this, e);
        } catch (IOException e) {
            log.error("Unable to load system information for radio " + this, e);
        }
    }

    public synchronized void loadJVMInformation() {
        try {
            jvmInfo = new NodeJVMStats(api.get(ClusterEntityJVMStatsResponse.class)
                    .path("/system/jvm")
                    .radio(this)
                    .execute());
        } catch (APIException e) {
            log.error("Unable to load JVM information for radio " + this, e);
        } catch (IOException e) {
            log.error("Unable to load JVM information for radio " + this, e);
        }
    }

    public synchronized void loadBufferInformation() {
        try {
            bufferInfo = new BufferInfo(api.get(BuffersResponse.class)
                    .path("/system/buffers")
                    .radio(this)
                    .execute());
        } catch (APIException e) {
            log.error("Unable to load buffer information for radio " + this, e);
        } catch (IOException e) {
            log.error("Unable to load buffer information for radio " + this, e);
        }
    }

    public String getNodeId() {
        return id;
    }

    @Override
    public String getShortNodeId() {
        return shortNodeId;
    }

    public String getId() {
        return id;
    }

    public NodeJVMStats jvm() {
        if (jvmInfo == null) {
            loadJVMInformation();
        }

        return jvmInfo;
    }

    public String getPid() {
        return jvm().getPid();
    }

    public String getJVMDescription() {
        return jvm().getInfo();
    }

    public void overrideLbStatus(String override) throws APIException, IOException {
        api.put()
                .path("/system/lbstatus/override/{0}", override)
                .radio(this)
                .execute();
    }

    public boolean launchExistingInput(String inputId) {
        try {
            api.get(InputLaunchResponse.class)
                    .path("/system/inputs/{0}/launch", inputId)
                    .radio(this)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (APIException e) {
            log.error("Could not launch input " + inputId, e);
        } catch (IOException e) {
            log.error("Could not launch input " + inputId, e);
        }

        return false;
    }

    @Override
    public boolean terminateInput(String inputId) {
        try {
            api.delete().path("/system/inputs/{0}", inputId)
                    .radio(this)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (APIException e) {
            log.error("Could not terminate input " + inputId, e);
        } catch (IOException e) {
            log.error("Could not terminate input " + inputId, e);
        }

        return false;
    }

    @Override
    public String getTransportAddress() {
        return transportAddress.toASCIIString();
    }

    public URI getTransportAddressUri() {
        return transportAddress;
    }

    @Override
    public String getHostname() {
        if (systemInfo == null) {
            loadSystemInformation();
        }

        return systemInfo.hostname;
    }

    public String getVersion() {
        if (systemInfo == null) {
            loadSystemInformation();
        }

        return systemInfo.version;
    }

    public String getLifecycle() {
        if (systemInfo == null) {
            loadSystemInformation();
        }

        return this.systemInfo.lifecycle;
    }

    public boolean lbAlive() {
        if (systemInfo == null) {
            loadSystemInformation();
        }

        return this.systemInfo.lbStatus != null && this.systemInfo.lbStatus.equals("alive");
    }

    @Override
    public void touch() {
        // We don't do touches against radios.
    }

    @Override
    public void markFailure() {
        // No failure counting in radios for now.
    }

    public Map<String, InputTypeSummaryResponse> getAllInputTypeInformation() throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        for (String type : getInputTypes().keySet()) {
            InputTypeSummaryResponse itr = getInputTypeInformation(type);
            types.put(itr.type, itr);
        }

        return types;
    }

    public Map<String, String> getInputTypes() throws IOException, APIException {
        return api.get(InputTypesResponse.class).radio(this).path("/system/inputs/types").execute().types;
    }

    public InputTypeSummaryResponse getInputTypeInformation(String type) throws IOException, APIException {
        return api.get(InputTypeSummaryResponse.class).radio(this).path("/system/inputs/types/{0}", type).execute();
    }

    public List<Input> getInputs() {
        List<Input> inputs = Lists.newArrayList();

        for (InputStateSummaryResponse input : inputs().inputs) {
            inputs.add(inputFactory.fromSummaryResponse(input.messageinput, this));
        }

        return inputs;
    }

    public Input getInput(String inputId) throws IOException, APIException {
        final InputSummaryResponse inputSummaryResponse = api.get(InputSummaryResponse.class).radio(this).path("/system/inputs/{0}", inputId).execute();
        return inputFactory.fromSummaryResponse(inputSummaryResponse, this);
    }


    public int numberOfInputs() {
        return inputs().total;
    }

    private InputsResponse inputs() {
        try {
            return api.get(InputsResponse.class).radio(this).path("/system/inputs").execute();
        } catch (Exception e) {
            Logger.error("Could not get inputs.", e);
            throw new RuntimeException("Could not get inputs.", e);
        }
    }

    @Override
    public InputLaunchResponse launchInput(String title, String type, Boolean global, Map<String, Object> configuration, User creator, boolean isExclusive) throws ExclusiveInputException {
        if (isExclusive) {
            for (Input input : getInputs()) {
                if (input.getType().equals(type)) {
                    throw new ExclusiveInputException();
                }
            }
        }

        InputLaunchRequest request = new InputLaunchRequest();
        request.title = title;
        request.type = type;
        request.global = global;
        request.configuration = configuration;
        request.creatorUserId = creator.getId();

        InputLaunchResponse ilr = null;
        try {
            ilr = api.post(InputLaunchResponse.class)
                    .path("/system/inputs")
                    .radio(this)
                    .body(request)
                    .expect(Http.Status.ACCEPTED)
                    .execute();

        } catch (APIException e) {
            log.error("Could not launch input " + title, e);
        } catch (IOException e) {
            log.error("Could not launch input " + title, e);
        }
        return ilr;
    }

    public BufferInfo getBuffers() {
        if (bufferInfo == null) {
            loadBufferInformation();
        }
        return bufferInfo;
    }

    public String getThreadDump() throws IOException, APIException {
        return api.get(String.class)
                .radio(this)
                .path("/system/threaddump")
                .accept(MediaType.ANY_TEXT_TYPE)
                .execute();
    }

    public int getThroughput() {
        try {
            return api.get(NodeThroughputResponse.class).radio(this).path("/system/throughput").execute().throughput;
        } catch (APIException e) {
            log.error("Could not load throughput for radio " + this, e);
        } catch (IOException e) {
            log.error("Could not load throughput for radio " + this, e);
        }
        return 0;
    }

    public Map<String, Metric> getMetrics(String namespace) throws APIException, IOException {
        MetricsListResponse response = api.get(MetricsListResponse.class)
                .radio(this)
                .path("/system/metrics/namespace/{0}", namespace)
                .expect(200, 404)
                .execute();

        return response.getMetrics();
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        if (id == null) {
            b.append("UnresolvedNode {'").append(transportAddress).append("'}");
            return b.toString();
        }

        b.append("Node {");
        b.append("'").append(id).append("'");
        b.append(", ").append(transportAddress);
        b.append("}");
        return b.toString();
    }

}
