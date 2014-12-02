/**
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
import org.graylog2.restclient.models.api.responses.SystemOverviewResponse;
import org.graylog2.restclient.models.api.responses.cluster.RadioSummaryResponse;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import org.graylog2.restclient.models.api.responses.system.ClusterEntityJVMStatsResponse;
import org.graylog2.restclient.models.api.responses.system.InputLaunchResponse;
import org.graylog2.restclient.models.api.responses.system.InputStateSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypeSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypesResponse;
import org.graylog2.restclient.models.api.responses.system.InputsResponse;
import org.graylog2.restclient.models.api.responses.system.NodeThroughputResponse;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class Radio extends ClusterEntity {

    public interface Factory {
        Radio fromSummaryResponse(RadioSummaryResponse r);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

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
        id = r.nodeId;
        shortNodeId = r.shortNodeId;
    }

    public synchronized void loadSystemInformation() {
        if (systemInfo != null) {
            return;
        }
        try {
            systemInfo = api.path(routes.radio().SystemResource().system(), SystemOverviewResponse.class)
                    .radio(this)
                    .execute();
        } catch (Exception e) {
            LOG.error("Unable to load system information for radio " + this, e);
        }
    }

    public synchronized void loadJVMInformation() {
        if (jvmInfo != null) {
            return;
        }
        try {
            jvmInfo = new NodeJVMStats(api.path(routes.radio().SystemResource().jvm(), ClusterEntityJVMStatsResponse.class)
                    .radio(this)
                    .execute());
        } catch (Exception e) {
            LOG.error("Unable to load JVM information for radio " + this, e);
        }
    }

    public synchronized void loadBufferInformation() {
        if (bufferInfo != null) {
            return;
        }
        try {
            bufferInfo = new BufferInfo(api.path(routes.radio().BuffersResource().utilization(), BuffersResponse.class)
                    .radio(this)
                    .execute());
        } catch (Exception e) {
            LOG.error("Unable to load buffer information for radio " + this, e);
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
        loadJVMInformation();

        if (jvmInfo == null) {
            return NodeJVMStats.buildEmpty();
        } else {
            return jvmInfo;
        }
    }

    public String getPid() {
        return jvm().getPid();
    }

    public String getJVMDescription() {
        return jvm().getInfo();
    }

    public void overrideLbStatus(String override) throws APIException, IOException {
        api.path(routes.radio().LoadBalancerStatusResource().override(override))
                .radio(this)
                .execute();
    }

    public boolean launchExistingInput(String inputId) {
        try {
            api.path(routes.radio().InputsResource().launchExisting(inputId), InputLaunchResponse.class)
                    .radio(this)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (Exception e) {
            LOG.error("Could not launch input " + inputId, e);
        }

        return false;
    }

    @Override
    public boolean terminateInput(String inputId) {
        try {
            api.path(routes.radio().InputsResource().terminate(inputId))
                    .radio(this)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (Exception e) {
            LOG.error("Could not terminate input " + inputId, e);
        }

        return false;
    }

    private SystemOverviewResponse systemInfo() {
        loadSystemInformation();

        if (systemInfo == null) {
            return SystemOverviewResponse.buildEmpty();
        } else {
            return systemInfo;
        }
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
        return systemInfo().hostname;
    }

    public String getVersion() {
        return systemInfo().version;
    }

    public String getLifecycle() {
        return this.systemInfo().lifecycle;
    }

    public boolean lbAlive() {
        final SystemOverviewResponse info = systemInfo();

        return info.lbStatus != null && info.lbStatus.equals("alive");
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
        return api.path(routes.radio().InputsResource().types(), InputTypesResponse.class).radio(this).execute().types;
    }

    public InputTypeSummaryResponse getInputTypeInformation(String type) throws IOException, APIException {
        return api.path(routes.radio().InputsResource().info(type), InputTypeSummaryResponse.class).radio(this).execute();
    }

    public List<Input> getInputs() {
        List<Input> inputs = Lists.newArrayList();

        for (InputStateSummaryResponse input : inputs().inputs) {
            inputs.add(inputFactory.fromSummaryResponse(input.messageinput, this));
        }

        return inputs;
    }

    public Input getInput(String inputId) throws IOException, APIException {
        final InputSummaryResponse inputSummaryResponse = api
                .path(routes.radio().InputsResource().single(inputId), InputSummaryResponse.class).radio(this).execute();
        return inputFactory.fromSummaryResponse(inputSummaryResponse, this);
    }


    public int numberOfInputs() {
        return inputs().total;
    }

    private InputsResponse inputs() {
        try {
            return api.path(routes.radio().InputsResource().list(), InputsResponse.class).radio(this).execute();
        } catch (Exception e) {
            LOG.error("Could not get inputs.", e);
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
        request.creatorUserId = creator.getName();

        InputLaunchResponse ilr = null;
        try {
            ilr = api.path(routes.radio().InputsResource().launch(), InputLaunchResponse.class)
                    .radio(this)
                    .body(request)
                    .expect(Http.Status.ACCEPTED)
                    .execute();

        } catch (Exception e) {
            LOG.error("Could not launch input " + title, e);
        }
        return ilr;
    }

    public BufferInfo getBuffers() {
        loadBufferInformation();

        if (bufferInfo == null) {
            return BufferInfo.buildEmpty();
        } else {
            return bufferInfo;
        }
    }

    public String getThreadDump() throws IOException, APIException {
        return api.path(routes.radio().SystemResource().threaddump(), String.class)
                .radio(this)
                .accept(MediaType.ANY_TEXT_TYPE)
                .execute();
    }

    public int getThroughput() {
        try {
            return api.path(routes.radio().ThroughputResource().total(), NodeThroughputResponse.class).radio(this).execute().throughput;
        } catch (Exception e) {
            LOG.error("Could not load throughput for radio " + this, e);
        }
        return 0;
    }

    public Map<String, Metric> getMetrics(String namespace) throws APIException, IOException {
        MetricsListResponse response = api.path(routes.radio().MetricsResource().byNamespace(namespace), MetricsListResponse.class)
                .radio(this)
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

    @Override
    public void stopInput(String inputId) throws IOException, APIException {
        api.path(routes.radio().InputsResource().stop(inputId))
                .radio(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    @Override
    public void startInput(String inputId) throws IOException, APIException {
        api.path(routes.radio().InputsResource().launchExisting(inputId))
                .radio(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    @Override
    public void restartInput(String inputId) throws IOException, APIException {
        api.path(routes.InputsResource().restart(inputId))
                .radio(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }
}
