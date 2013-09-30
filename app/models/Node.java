/*
 * Copyright 2013 TORCH UG
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
 */
package models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import lib.ExclusiveInputException;
import models.api.requests.InputLaunchRequest;
import models.api.responses.BuffersResponse;
import models.api.responses.NodeSummaryResponse;
import models.api.responses.SystemOverviewResponse;
import models.api.responses.system.*;
import models.api.responses.system.loggers.LoggerSummary;
import models.api.responses.system.loggers.LoggersResponse;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import play.Logger;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Node {

    public interface Factory {
        Node fromSummaryResponse(NodeSummaryResponse r);
    }
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Node.class);

    private final ApiClient api;

    private final Input.Factory inputFactory;
    private final String transportAddress;
    private final DateTime lastSeen;
    private final String nodeId;
    private final String shortNodeId;

    private final boolean isMaster;
    private SystemOverviewResponse systemInfo;

    /* for initial set up in test */
    public Node(NodeSummaryResponse r) {
        this(null, null, r);
    }

    @AssistedInject
    public Node(ApiClient api, Input.Factory inputFactory, @Assisted NodeSummaryResponse r) {
        this.api = api;
        this.inputFactory = inputFactory;

        transportAddress = r.transportAddress;
        lastSeen = new DateTime(r.lastSeen);
        nodeId = r.nodeId;
        shortNodeId = r.shortNodeId;
        isMaster = r.isMaster;
    }

    public BufferInfo getBufferInfo() {
        try {
            return new BufferInfo(
                    api.get(BuffersResponse.class)
                    .node(this)
                    .path("/system/buffers")
                    .execute());
        } catch (APIException e) {
            log.error("Unable to read buffer info from node " + this, e);
        } catch (IOException e) {
            log.error("Unexpected exception", e);
        }
        return null;
    }

    public List<InternalLogger> allLoggers() {
        List<InternalLogger> loggers = Lists.newArrayList();
        try {
            LoggersResponse response = api.get(LoggersResponse.class)
                    .node(this)
                    .path("/system/loggers")
                    .execute();

            for (Map.Entry<String, LoggerSummary> logger : response.loggers.entrySet()) {
                loggers.add(new InternalLogger(logger.getKey(), logger.getValue().level, logger.getValue().syslogLevel));
            }
        } catch (APIException e) {
            log.error("Unable to load loggers for node " + this, e);
        } catch (IOException e) {
            log.error("Unable to load loggers for node " + this, e);
        }
        return loggers;
    }

    public String getThreadDump() throws IOException, APIException {
        return api.get(String.class).node(this).path("/system/threaddump").execute();
    }

    public List<Input> getInputs() {
        List<Input> inputs = Lists.newArrayList();

        for (InputSummaryResponse input : inputs().inputs) {
            inputs.add(inputFactory.fromSummaryResponse(input));
        }

        return inputs;
    }

    public Input getInput(String inputId) throws IOException, APIException {
        final InputSummaryResponse inputSummaryResponse = api.get(InputSummaryResponse.class).node(this).path("/system/inputs/{0}", inputId).execute();
        return inputFactory.fromSummaryResponse(inputSummaryResponse);
    }

    public int numberOfInputs() {
        return inputs().total;
    }

    public boolean launchInput(String title, String type, Map<String, Object> configuration, User creator, boolean isExclusive) throws ExclusiveInputException {
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
        request.configuration = configuration;
        request.creatorUserId = creator.getId();

        try {
            api.post()
                    .path("/system/inputs")
                    .node(this)
                    .body(request)
                    .expect(Http.Status.ACCEPTED)
                    .execute();
            return true;
        } catch (APIException e) {
            log.error("Could not launch input " + title, e);
        } catch (IOException e) {
            log.error("Could not launch input " + title, e);
        }
        return false;
    }

    public boolean terminateInput(String inputId) {
        try {
            api.delete().path("/system/inputs/{0}", inputId)
                    .node(this)
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

    public Map<String, String> getInputTypes() throws IOException, APIException {
        return api.get(InputTypesResponse.class).node(this).path("/system/inputs/types").execute().types;
    }

    public InputTypeSummaryResponse getInputTypeInformation(String type) throws IOException, APIException {
        return api.get(InputTypeSummaryResponse.class).node(this).path("/system/inputs/types/{0}", type).execute();
    }

    public Map<String, InputTypeSummaryResponse> getAllInputTypeInformation() throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        for (String type : getInputTypes().keySet()) {
            InputTypeSummaryResponse itr = getInputTypeInformation(type);
            types.put(itr.type, itr);
        }

        return types;
    }

    public synchronized void loadSystemInformation() {
        try {
            systemInfo = api.get(SystemOverviewResponse.class).path("/system").node(this).execute();
        } catch (APIException e) {
            log.error("Unable to load system information for node " + this, e);
        } catch (IOException e) {
            log.error("Unable to load system information for node " + this, e);
        }
    }

    public String getTransportAddress() {
        return transportAddress;
    }

    public DateTime getLastSeen() {
        return lastSeen;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getShortNodeId() {
        return shortNodeId;
    }

    public String getHostname() {
        if (systemInfo == null) {
            loadSystemInformation();
        }
        return systemInfo.hostname;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public boolean isProcessing() {
        if (systemInfo == null) {
            loadSystemInformation();
        }
        return systemInfo.isProcessing;
    }

    public void pause() throws IOException, APIException {
        api.put()
            .path("/system/processing/pause")
            .node(this)
            .execute();
    }

    public void resume() throws IOException, APIException {
        api.put()
            .path("/system/processing/resume")
            .node(this)
            .execute();
    }

    public int getThroughput() {
        try {
            return api.get(ServerThroughputResponse.class).node(this).path("/system/throughput").execute().throughput;
        } catch (APIException e) {
            log.error("Could not load throughput for node " + this, e);
        } catch (IOException e) {
            log.error("Could not load throughput for node " + this, e);
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
            return api.get(InputsResponse.class).node(this).path("/system/inputs").execute();
        } catch (Exception e) {
            Logger.error("Could not get inputs.", e);
            throw new RuntimeException("Could not get inputs.", e);
        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeId='" + nodeId + '\'' +
                ", transportAddress='" + transportAddress + '\'' +
                ", isMaster=" + isMaster +
                '}';
    }
}
