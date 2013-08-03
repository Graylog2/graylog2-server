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
import lib.APIException;
import lib.Api;
import lib.Configuration;
import models.api.responses.*;
import models.api.responses.system.InputSummaryResponse;
import models.api.responses.system.InputsResponse;
import org.joda.time.DateTime;
import play.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Node {

    private static final Random randomGenerator = new Random();

    private final String transportAddress;
    private final DateTime lastSeen;
    private final String nodeId;
    private final String shortNodeId;
    private final String hostname;
    private final boolean isMaster;

    public Node(NodeSummaryResponse r) {
        transportAddress = r.transportAddress;
        lastSeen = new DateTime(r.lastSeen);
        nodeId = r.nodeId;
        shortNodeId = r.shortNodeId;
        hostname = r.hostname;
        isMaster = r.isMaster;
    }

    public static Node fromId(String id) {
        NodeSummaryResponse response = null;
        try {
            response = Api.get(
                    Configuration.getServerRestUris().get(0),
                    "/cluster/nodes/" + id,
                    NodeSummaryResponse.class);
        } catch (IOException e) {
            return null;
        } catch (APIException e) {
            return null;
        }

        return new Node(response);
    }

    public static List<Node> all() throws IOException, APIException {
        List<Node> nodes = Lists.newArrayList();

        NodeResponse response = Api.get(Configuration.getServerRestUris().get(0), "/cluster/nodes/", NodeResponse.class);
        for (NodeSummaryResponse nsr : response.nodes) {
            nodes.add(new Node(nsr));
        }

        return nodes;
    }

    public static Map<String, Node> map() throws IOException, APIException {
        Map<String, Node> map = Maps.newHashMap();
        for (Node node : all()) {
            map.put(node.getNodeId(), node);
        }

        return map;
    }

    public static Node random() throws IOException, APIException {
        List<Node> nodes = all();
        return all().get(randomGenerator.nextInt(nodes.size()));
    }

    public String getThreadDump() throws IOException, APIException {
        return Api.get(this, "/system/threaddump", String.class);
    }

    public List<Input> getInputs() {
        List<Input> inputs = Lists.newArrayList();

        for (InputSummaryResponse input : inputs().inputs) {
            inputs.add(new Input(input));
        }

        return inputs;
    }

    public Input getInput(String inputId) throws IOException, APIException {
        return new Input(Api.get(this, "/system/inputs/" + inputId, InputSummaryResponse.class));
    }

    public int numberOfInputs() {
        return inputs().total;
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
        return hostname;
    }

    public boolean isMaster() {
        return isMaster;
    }

    /**
     * This swallows all exceptions to allow easy lazy-loading in views without exception handling.
     *
     * @return List of running inputs o this node.
     */
    private InputsResponse inputs()  {
        try {
            return Api.get(this, "/system/inputs", InputsResponse.class);
        } catch (Exception e) {
            Logger.error("Could not get inputs.", e);
            throw new RuntimeException("Could not get inputs.", e);
        }
    }
}
