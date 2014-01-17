package models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.ExclusiveInputException;
import lib.ServerNodes;
import models.api.requests.InputLaunchRequest;
import models.api.responses.system.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
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
        result.putAll(api.get(InputsResponse.class).fromAllNodes().path("/system/inputs").executeOnAll());
        try {
            for(Radio radio : nodeService.radios().values()) {
                result.put(radio, api.get(InputsResponse.class).radio(radio).path("/system/inputs").execute());
            }
        } catch (APIException e) {
            log.error("Unable to fetch radio list: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch radio list: " + e);
        }
        return result;
    }

    protected List<InputStateSummaryResponse> getInputsFromNode(Node node) {
        List<InputStateSummaryResponse> result = Lists.newArrayList();
        try {
            result = api.get(InputsResponse.class).node(node).path("/system/inputs").execute().inputs;
        } catch (APIException e) {
            log.error("Unable to fetch input list: " + e);
        } catch (IOException e) {
            log.error("Unable to fetch input list: " + e);
        }

        return result;
    }

    protected Map<Node, InputsResponse> getInputsFromAllNodes() {
        return api.get(InputsResponse.class).fromAllNodes().path("/system/inputs").executeOnAll();
    }

    public List<InputState> loadAllInputStates(Node node) {
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
        Map<Node, InputTypesResponse> inputTypesResponseMap = api.get(InputTypesResponse.class)
                .fromAllNodes().path("/system/inputs/types").executeOnAll();

        for (Map.Entry<Node, InputTypesResponse> entry : inputTypesResponseMap.entrySet())
            result.put(entry.getKey(), entry.getValue().types);

        return result;
    }

    public InputTypeSummaryResponse getInputTypeInformation(Node node, String type) throws IOException, APIException {
        return api.get(InputTypeSummaryResponse.class).node(node).path("/system/inputs/types/{0}", type).execute();
    }

    public Map<String, InputTypeSummaryResponse> getAllInputTypeInformation() throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        for (Map.Entry<Node, Map<String, String>> entry : getAllInputTypes().entrySet()) {
            for (String type : entry.getValue().keySet()) {
                InputTypeSummaryResponse itr = getInputTypeInformation(entry.getKey(), type);
                types.put(itr.type, itr);
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

}
