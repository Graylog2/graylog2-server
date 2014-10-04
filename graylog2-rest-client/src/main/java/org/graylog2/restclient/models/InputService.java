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
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ExclusiveInputException;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.api.requests.InputLaunchRequest;
import org.graylog2.restclient.models.api.responses.system.*;
import org.graylog2.restroutes.generated.InputsResource;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputService {
    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final ApiClient api;
    private final Input.Factory inputFactory;
    private final InputState.Factory inputStateFactory;
    private final NodeService nodeService;
    private final ServerNodes serverNodes;
    private final InputsResource resource = routes.InputsResource();


    @Inject
    public InputService(ApiClient api,
                        Input.Factory inputFactory,
                        InputState.Factory inputStateFactory,
                        NodeService nodeService,
                        ServerNodes serverNodes) {
        this.api = api;
        this.inputFactory = inputFactory;
        this.inputStateFactory = inputStateFactory;
        this.nodeService = nodeService;
        this.serverNodes = serverNodes;
    }

    protected Map<ClusterEntity, InputsResponse> getInputsFromAllEntities() {
        Map<ClusterEntity, InputsResponse> result = Maps.newHashMap();
        result.putAll(api.path(resource.list(), InputsResponse.class).fromAllNodes().executeOnAll());
        try {
            for(Radio radio : nodeService.radios().values()) {
                result.put(radio,
                        api.path(routes.radio().InputsResource().list(), InputsResponse.class).radio(radio).execute());
            }
        } catch (APIException e) {
            log.error("Unable to fetch radio list: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch radio list: " + e);
        }
        return result;
    }

    protected List<InputStateSummaryResponse> getInputsFromNode(ClusterEntity node) {
        List<InputStateSummaryResponse> result = Lists.newArrayList();
        try {
            result = api.path(resource.list(), InputsResponse.class).clusterEntity(node).execute().inputs;
        } catch (APIException e) {
            log.error("Unable to fetch input list: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch input list: " + e);
        }

        return result;
    }

    protected Map<Node, InputsResponse> getInputsFromAllNodes() {
        return api.path(resource.list(), InputsResponse.class).fromAllNodes().executeOnAll();
    }

    public List<InputState> loadAllInputStates(ClusterEntity node) {
        List<InputState> inputStates = Lists.newArrayList();

        for (InputStateSummaryResponse inputsResponse : getInputsFromNode(node)) {
            inputStates.add(inputStateFactory.fromSummaryResponse(inputsResponse, node));
        }

        return inputStates;
    }

    public List<InputState> loadAllInputStates() {
        List<InputState> inputStates = Lists.newArrayList();

        for (Map.Entry<ClusterEntity, List<InputState>> entry : loadAllInputStatesByEntity().entrySet()) {
            inputStates.addAll(entry.getValue());
        }

        return inputStates;
    }

    public Map<ClusterEntity, List<InputState>> loadAllInputStatesByEntity() {
        Map<ClusterEntity, List<InputState>> result = Maps.newHashMap();
        Map<ClusterEntity, InputsResponse> inputsResponseMap = getInputsFromAllEntities();

        for (Map.Entry<ClusterEntity, InputsResponse> entry : inputsResponseMap.entrySet()) {
            List<InputState> nodeList = Lists.newArrayList();
            result.put(entry.getKey(), nodeList);

            for (InputStateSummaryResponse issr : entry.getValue().inputs) {
                nodeList.add(inputStateFactory.fromSummaryResponse(issr, entry.getKey()));
            }
        }

        return result;
    }

    public Map<Input, Map<ClusterEntity, InputState>> loadAllInputStatesByInput() {
        Map<ClusterEntity, List<InputState>> inputStatesByNode = loadAllInputStatesByEntity();
        Map<Input, Map<ClusterEntity, InputState>> result = Maps.newHashMap();

        for (Map.Entry<ClusterEntity, List<InputState>> nodeEntry : inputStatesByNode.entrySet()) {
            for (InputState inputState : nodeEntry.getValue()) {
                Input input = inputState.getInput();
                if (result.get(input) == null) {
                    Map<ClusterEntity, InputState> inputStateMap = Maps.newHashMap();
                    result.put(input, inputStateMap);
                }

                result.get(input).put(nodeEntry.getKey(), inputState);
            }
        }

        return result;
    }

    public Map<Node, Map<String, String>> getAllInputTypes() throws IOException, APIException {
        Map<Node, Map<String, String>> result = Maps.newHashMap();
        Map<Node, InputTypesResponse> inputTypesResponseMap = api.path(resource.types(), InputTypesResponse.class)
                .fromAllNodes().executeOnAll();

        for (Map.Entry<Node, InputTypesResponse> entry : inputTypesResponseMap.entrySet())
            result.put(entry.getKey(), entry.getValue().types);

        return result;
    }

    public InputTypeSummaryResponse getInputTypeInformation(Node node, String type) throws IOException, APIException {
        return api.path(resource.info(type), InputTypeSummaryResponse.class).node(node).execute();
    }

    public Map<String, InputTypeSummaryResponse> getAllInputTypeInformation() throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        for (Map.Entry<Node, Map<String, String>> entry : getAllInputTypes().entrySet()) {
            for (String type : entry.getValue().keySet()) {
                try {
                    InputTypeSummaryResponse itr = getInputTypeInformation(entry.getKey(), type);
                    if (itr != null && itr.type != null)
                        types.put(itr.type, itr);
                } catch (IOException | APIException e) {
                    log.error("Unable to get details for input <" + type + ">: ", e);
                }
            }
        }

        return types;
    }

    public String launchGlobal(String title, String type, Map<String, Object> configuration, User creator, boolean isExclusive) throws ExclusiveInputException {
        Node master = serverNodes.master();
        InputLaunchResponse ilr = master.launchInput(title, type, true, configuration, creator, isExclusive);

        if (ilr == null) {
            throw new RuntimeException("Unable to launch global input!");
        }

        for (Node serverNode: serverNodes.all()) {
            if (!serverNode.isMaster())
                serverNode.launchExistingInput(ilr.persistId);
        }
        try {
            for (Radio radio : nodeService.radios().values()) {
                radio.launchExistingInput(ilr.persistId);
            }
        } catch (APIException e) {
            log.error("Unable to fetch list of radios: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch list of radios: " + e);
        }

        return ilr.persistId;
    }

    public Map<ClusterEntity, Boolean> terminateGlobal(String inputId) {
        Map<ClusterEntity, Boolean> results = Maps.newHashMap();

        for (Node serverNode : serverNodes.all())
            if (!serverNode.isMaster())
                results.put(serverNode, serverNode.terminateInput(inputId));

        try {
            for (Radio radio : nodeService.radios().values())
                results.put(radio, radio.terminateInput(inputId));
        } catch (APIException e) {
            log.error("Unable to fetch list of radios: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch list of radios: " + e);
        }

        Node master = serverNodes.master();
        results.put(master, master.terminateInput(inputId));

        return results;
    }

    public void start(String inputId) throws IOException, APIException {
        Map.Entry<ClusterEntity, InputState> target = findNodeAndInputStateForInput(inputId);

        List<ClusterEntity> targetNodes = targetNodesForInput(target);

        for (ClusterEntity targetNode : targetNodes)
            targetNode.startInput(inputId);
    }

    public void stop(String inputId) throws IOException, APIException {
        final Map.Entry<ClusterEntity, InputState> target = findNodeAndInputStateForInput(inputId);

        List<ClusterEntity> targetNodes = targetNodesForInput(target);

        for (ClusterEntity targetNode : targetNodes)
            targetNode.stopInput(inputId);
    }

    public void restart(String inputId) throws IOException, APIException {
        final Map.Entry<ClusterEntity, InputState> target = findNodeAndInputStateForInput(inputId);

        List<ClusterEntity> targetNodes = targetNodesForInput(target);

        for (ClusterEntity targetNode : targetNodes)
            targetNode.restartInput(inputId);
    }

    protected List<ClusterEntity> targetNodesForInput(Map.Entry<ClusterEntity, InputState> target) {
        final List<ClusterEntity> targetNodes = Lists.newArrayList();
        if (target.getValue().getInput().getGlobal()) {
            targetNodes.addAll(serverNodes.all());
            try {
                targetNodes.addAll(nodeService.radios().values());
            } catch (APIException | IOException e) {
                log.error("Unable to fetch list of radios: " + e);
            }
        } else {
            targetNodes.add(target.getKey());
        }

        return targetNodes;
    }

    protected Map.Entry<ClusterEntity, InputState> findNodeAndInputStateForInput(String inputId) {
        ClusterEntity targetNode = null;
        InputState targetInputState = null;

        for (final Map.Entry<ClusterEntity, List<InputState>> entry : loadAllInputStatesByEntity().entrySet()) {
            for (final InputState inputState : entry.getValue()) {
                if (inputState.getInput().getPersistId().equals(inputId)) {
                    return new AbstractMap.SimpleImmutableEntry<ClusterEntity, InputState>(entry.getKey(), inputState);
                }
            }
        }

        return null;
    }
}
