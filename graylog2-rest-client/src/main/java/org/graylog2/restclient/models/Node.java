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
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.lib.ExclusiveInputException;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.models.api.requests.InputLaunchRequest;
import org.graylog2.restclient.models.api.responses.BufferClassesResponse;
import org.graylog2.restclient.models.api.responses.BuffersResponse;
import org.graylog2.restclient.models.api.responses.SystemOverviewResponse;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import org.graylog2.restclient.models.api.responses.system.ClusterEntityJVMStatsResponse;
import org.graylog2.restclient.models.api.responses.system.InputLaunchResponse;
import org.graylog2.restclient.models.api.responses.system.InputStateSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypeSummaryResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypesResponse;
import org.graylog2.restclient.models.api.responses.system.InputsResponse;
import org.graylog2.restclient.models.api.responses.system.NodeThroughputResponse;
import org.graylog2.restclient.models.api.responses.system.loggers.LoggerSubsystemSummary;
import org.graylog2.restclient.models.api.responses.system.loggers.LoggerSubsystemsResponse;
import org.graylog2.restclient.models.api.responses.system.loggers.LoggerSummary;
import org.graylog2.restclient.models.api.responses.system.loggers.LoggersResponse;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.MoreObjects.firstNonNull;

public class Node extends ClusterEntity {

    public interface Factory {
        Node fromSummaryResponse(NodeSummaryResponse r);

        Node fromTransportAddress(URI transportAddress);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private final ApiClient api;
    private final Input.Factory inputFactory;
    private final InputState.Factory inputStateFactory;
    private final URI transportAddress;

    private DateTime lastSeen;
    private DateTime lastContact;
    private String nodeId;
    private boolean isMaster;
    private String shortNodeId;
    private AtomicBoolean active = new AtomicBoolean();

    private final boolean fromConfiguration;
    private SystemOverviewResponse systemInfo;
    private NodeJVMStats jvmInfo;

    private AtomicInteger failureCount = new AtomicInteger(0);

    /* for initial set up in test */
    public Node(NodeSummaryResponse r) {
        this(null, null, null, r);
    }

    @AssistedInject
    public Node(ApiClient api,
                Input.Factory inputFactory,
                InputState.Factory inputStateFactory,
                @Assisted NodeSummaryResponse r) {
        this.api = api;
        this.inputFactory = inputFactory;
        this.inputStateFactory = inputStateFactory;

        transportAddress = normalizeUriPath(r.transportAddress);
        lastSeen = new DateTime(r.lastSeen, DateTimeZone.UTC);
        nodeId = r.nodeId;
        shortNodeId = r.shortNodeId;
        isMaster = r.isMaster;
        fromConfiguration = false;
    }

    @AssistedInject
    public Node(ApiClient api,
                Input.Factory inputFactory,
                InputState.Factory inputStateFactory,
                @Assisted URI transportAddress) {
        this.api = api;
        this.inputFactory = inputFactory;
        this.inputStateFactory = inputStateFactory;

        this.transportAddress = normalizeUriPath(transportAddress);
        lastSeen = null;
        nodeId = null;
        shortNodeId = "unresolved";
        isMaster = false;
        fromConfiguration = true;
    }

    public BufferInfo getBufferInfo() {
        try {
            return new BufferInfo(
                    api.path(routes.BufferResource().utilization(), BuffersResponse.class)
                            .node(this)
                            .execute());
        } catch (Exception e) {
            LOG.error("Unable to read buffer info from node " + this, e);
        }
        return null;
    }

    public BufferClassesResponse getBufferClasses() {
        try {
            return api.path(routes.BufferResource().getBufferClasses(), BufferClassesResponse.class).node(this).execute();
        } catch (Exception e) {
            LOG.error("Unable to read buffer class names from node " + this, e);
        }
        return null;
    }

    public Map<String, InternalLoggerSubsystem> allLoggerSubsystems() {
        Map<String, InternalLoggerSubsystem> subsystems = Maps.newHashMap();
        try {
            LoggerSubsystemsResponse response = api.path(routes.LoggersResource().subsytems(), LoggerSubsystemsResponse.class)
                    .node(this)
                    .execute();

            for (Map.Entry<String, LoggerSubsystemSummary> ss : response.subsystems.entrySet()) {
                subsystems.put(ss.getKey(), new InternalLoggerSubsystem(
                        ss.getValue().title,
                        ss.getValue().level,
                        ss.getValue().levelSyslog
                ));
            }
        } catch (Exception e) {
            LOG.error("Unable to load subsystems for node " + this, e);
        }
        return subsystems;
    }

    public List<InternalLogger> allLoggers() {
        List<InternalLogger> loggers = Lists.newArrayList();
        try {
            LoggersResponse response = api.path(routes.LoggersResource().loggers(), LoggersResponse.class)
                    .node(this)
                    .execute();

            for (Map.Entry<String, LoggerSummary> logger : response.loggers.entrySet()) {
                loggers.add(new InternalLogger(logger.getKey(), logger.getValue().level, logger.getValue().syslogLevel));
            }
        } catch (Exception e) {
            LOG.error("Unable to load loggers for node " + this, e);
        }
        return loggers;
    }

    public void setSubsystemLoggerLevel(String subsystem, String level) throws APIException, IOException {
        api.path(routes.LoggersResource().setSubsystemLoggerLevel(subsystem, level))
                .node(this)
                .execute();
    }

    public String getThreadDump() throws IOException, APIException {
        return api.path(routes.SystemResource().threaddump(), String.class)
                .node(this)
                .accept(MediaType.ANY_TEXT_TYPE)
                .execute();
    }

    public List<InputState> getInputStates() {
        List<InputState> inputStates = Lists.newArrayList();
        for (InputStateSummaryResponse issr : inputs().inputs) {
            inputStates.add(inputStateFactory.fromSummaryResponse(issr, this));
        }
        return inputStates;
    }

    public List<Input> getInputs() {
        List<Input> inputs = Lists.newArrayList();

        for (InputState input : getInputStates()) {
            inputs.add(input.getInput());
        }

        return inputs;
    }

    public Input getInput(String inputId) throws IOException, APIException {
        final InputSummaryResponse inputSummaryResponse = api.path(routes.InputsResource().single(inputId), InputSummaryResponse.class).node(this).execute();
        return inputFactory.fromSummaryResponse(inputSummaryResponse, this);
    }

    public int numberOfInputs() {
        return inputs().total;
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
            ilr = api.path(routes.InputsResource().create(), InputLaunchResponse.class)
                    .node(this)
                    .body(request)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
        } catch (Exception e) {
            LOG.error("Could not launch input " + title, e);
        }
        return ilr;
    }

    public boolean launchExistingInput(String inputId) {
        try {
            api.path(routes.InputsResource().launchExisting(inputId))
                    .node(this)
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
            api.path(routes.InputsResource().terminate(inputId))
                    .node(this)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (Exception e) {
            LOG.error("Could not terminate input " + inputId, e);
        }

        return false;
    }

    public Map<String, String> getInputTypes() throws IOException, APIException {
        return api.path(routes.InputsResource().types(), InputTypesResponse.class).node(this).execute().types;
    }

    public InputTypeSummaryResponse getInputTypeInformation(String type) throws IOException, APIException {
        return api.path(routes.InputsResource().info(type), InputTypeSummaryResponse.class).node(this).execute();
    }

    public Map<String, InputTypeSummaryResponse> getAllInputTypeInformation() throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        for (String type : getInputTypes().keySet()) {
            InputTypeSummaryResponse itr = getInputTypeInformation(type);
            types.put(itr.type, itr);
        }

        return types;
    }

    // TODO nodes should not have state beyond their activity status
    public synchronized SystemOverviewResponse loadSystemInformation() {
        try {
            return api.path(routes.SystemResource().system(), SystemOverviewResponse.class)
                    .node(this)
                    .execute();
        } catch (Exception e) {
            LOG.error("Unable to load system information for node " + this, e);
            return null;
        }
    }

    public synchronized NodeJVMStats loadJVMInformation() {
        try {
            return new NodeJVMStats(api.path(routes.SystemResource().jvm(), ClusterEntityJVMStatsResponse.class)
                    .node(this)
                    .execute()
            );
        } catch (Exception e) {
            LOG.error("Unable to load JVM information for node " + this, e);
            return null;
        }
    }

    @Override
    public String getTransportAddress() {
        return transportAddress.toASCIIString();
    }

    public URI getTransportAddressUri() {
        return transportAddress;
    }

    public DateTime getLastSeen() {
        return lastSeen;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getShortNodeId() {
        return shortNodeId;
    }

    @Override
    public String getHostname() {
        requireSystemInfo();
        return systemInfo.hostname;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public boolean isProcessing() {
        requireSystemInfo();
        return this.systemInfo.isProcessing;
    }

    public String getLifecycle() {
        requireSystemInfo();
        return this.systemInfo.lifecycle;
    }

    public boolean lbAlive() {
        requireSystemInfo();
        return this.systemInfo.lbStatus != null && this.systemInfo.lbStatus.equals("alive");
    }

    public String getVersion() {
        requireSystemInfo();
        return systemInfo.version;
    }

    public String getCodename() {
        requireSystemInfo();
        return systemInfo.codename;
    }

    public String getTimezone() {
        requireSystemInfo();
        return systemInfo.timezone;
    }

    public String getPid() {
        requireJVMInfo();
        return jvmInfo.getPid();
    }

    public String getJVMDescription() {
        requireJVMInfo();
        return jvmInfo.getInfo();
    }

    public NodeJVMStats jvm() {
        requireJVMInfo();
        return jvmInfo;
    }

    public Map<String, Metric> getMetrics(String namespace) throws APIException, IOException {
        MetricsListResponse response = api.path(routes.MetricsResource().byNamespace(namespace), MetricsListResponse.class)
                .node(this)
                .expect(200)
                .execute();
        if (response == null) {
            return Collections.emptyMap();
        }
        return response.getMetrics();
    }

    public Metric getSingleMetric(String metricName) throws APIException, IOException {
        return getMetrics(metricName).get(metricName);
    }

    public void pause() throws IOException, APIException {
        api.path(routes.SystemResource().pauseProcessing())
                .node(this)
                .execute();
    }

    public void resume() throws IOException, APIException {
        api.path(routes.SystemResource().resumeProcessing())
                .node(this)
                .execute();
    }

    public void overrideLbStatus(String override) throws APIException, IOException {
        api.path(routes.LoadBalancerStatusResource().override(override))
                .node(this)
                .execute();
    }

    public int getThroughput() {
        try {
            return api.path(routes.ThroughputResource().total(), NodeThroughputResponse.class).node(this).execute().throughput;
        } catch (Exception e) {
            LOG.error("Could not load throughput for node " + this, e);
        }
        return 0;
    }

    /**
     * This swallows all exceptions to allow easy lazy-loading in views without exception handling.
     *
     * @return List of running inputs o this node.
     */
    private InputsResponse inputs() {
        try {
            return api.path(routes.InputsResource().list(), InputsResponse.class).node(this).execute();
        } catch (Exception e) {
            LOG.error("Could not get inputs.", e);
            throw new RuntimeException("Could not get inputs.", e);
        }
    }

    public boolean isFromConfiguration() {
        return fromConfiguration;
    }

    @Override
    public void markFailure() {
        failureCount.incrementAndGet();
        setActive(false);
        LOG.info("{} failed, marking as inactive.", this);
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public DateTime getLastContact() {
        return lastContact;
    }

    public void merge(Node updatedNode) {
        LOG.debug("Merging node {} in this node {}", updatedNode, this);
        this.lastSeen = updatedNode.lastSeen;
        this.isMaster = updatedNode.isMaster;
        this.nodeId = updatedNode.nodeId;
        this.shortNodeId = updatedNode.shortNodeId;
        this.setActive(updatedNode.isActive());
    }

    @Override
    public void touch() {
        this.lastContact = DateTools.nowInUTC();
        setActive(true);
    }

    public boolean isActive() {
        return active.get();
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public void shutdown() throws APIException, IOException {
        api.path(routes.SystemResource().shutdown())
                .node(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        // if both have a node id, and they are the same, the nodes are the same.
        if (nodeId != null && node.nodeId != null) {
            if (nodeId.equals(node.nodeId)) {
                return true;
            }
        }
        // otherwise if the transport addresses are the same, we consider the nodes to be the same.
        if (transportAddress.equals(node.transportAddress)) return true;

        // otherwise the nodes aren't the same
        return false;
    }

    @Override
    public int hashCode() {
        int result = transportAddress.hashCode();
        result = 31 * result + (nodeId != null ? nodeId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder b = new StringBuilder();
        if (nodeId == null) {
            b.append("UnresolvedNode {'").append(transportAddress).append("'}");
            return b.toString();
        }

        b.append("Node {");
        b.append("'").append(nodeId).append("'");
        b.append(", ").append(transportAddress);
        if (isMaster) {
            b.append(", master");
        }
        if (isActive()) {
            b.append(", active");
        } else {
            b.append(", inactive");
        }
        final int failures = getFailureCount();
        if (failures > 0) {
            b.append(", failed: ").append(failures).append(" times");
        }
        b.append("}");
        return b.toString();
    }

    public void requireSystemInfo() {
        this.systemInfo = firstNonNull(loadSystemInformation(), SystemOverviewResponse.buildEmpty());
    }

    public void requireJVMInfo() {
        this.jvmInfo = firstNonNull(loadJVMInformation(), NodeJVMStats.buildEmpty());
    }

    @Override
    public void stopInput(String inputId) throws IOException, APIException {
        api.path(routes.InputsResource().stop(inputId))
                .node(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    @Override
    public void startInput(String inputId) throws IOException, APIException {
        api.path(routes.InputsResource().launchExisting(inputId))
                .node(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    @Override
    public void restartInput(String inputId) throws IOException, APIException {
        api.path(routes.InputsResource().restart(inputId))
                .node(this)
                .expect(Http.Status.ACCEPTED)
                .execute();
    }
}
