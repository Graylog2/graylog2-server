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
import models.api.responses.SystemOverviewResponse;
import models.api.responses.cluster.RadioSummaryResponse;
import models.api.responses.system.*;
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

    @Override
    public String getHostname() {
        if (systemInfo == null) {
            loadSystemInformation();
        }
        return systemInfo.hostname;
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

        for (InputSummaryResponse input : inputs().inputs) {
            inputs.add(inputFactory.fromSummaryResponse(input, this));
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
                    .radio(this)
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

    public BufferInfo getBuffers() {
        if (bufferInfo == null) {
            loadBufferInformation();
        }
        return bufferInfo;
    }

    public String getThreadDump() throws IOException, APIException {
        return api.get(String.class).radio(this).path("/system/threaddump").execute();
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
