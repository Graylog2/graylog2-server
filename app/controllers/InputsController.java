/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lib.BreadcrumbList;
import lib.security.RestPermissions;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ExclusiveInputException;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.InputService;
import org.graylog2.restclient.models.InputState;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.api.requests.inputs.LaunchInputRequest;
import org.graylog2.restclient.models.api.responses.system.InputLaunchResponse;
import org.graylog2.restclient.models.api.responses.system.InputTypeSummaryResponse;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Result;
import views.helpers.Permissions;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class InputsController extends AuthenticatedController {

    public static final Form<LaunchInputRequest> launchInputRequestForm = Form.form(LaunchInputRequest.class);

    private final NodeService nodeService;
    private final InputService inputService;
    private final ServerNodes servernodes;

    @Inject
    public InputsController(NodeService nodeService, InputService inputService, ServerNodes servernodes) {
        this.nodeService = nodeService;
        this.inputService = inputService;
        this.servernodes = servernodes;
    }

    public Result index() {
        try {
            if (!Permissions.isPermitted(RestPermissions.INPUTS_READ)) {
                return redirect(routes.StartpageController.redirect());
            }
            final Map<Input, Map<ClusterEntity, InputState>> globalInputs = Maps.newHashMap();
            final List<InputState> localInputs = Lists.newArrayList();

            for (InputState inputState : inputService.loadAllInputStates()) {
                if (inputState.getInput().getGlobal() == false)
                    localInputs.add(inputState);
            }

            for (Map.Entry<Input, Map<ClusterEntity, InputState>> entry : inputService.loadAllInputStatesByInput().entrySet()) {
                if (entry.getKey().getGlobal()) {
                    final SortedMap<ClusterEntity, InputState> inputStates = ImmutableSortedMap.copyOf(entry.getValue());
                    globalInputs.put(entry.getKey(), inputStates);
                }
            }

            List<Node> nodes = servernodes.all();
            List<Radio> radios = Lists.newArrayList();
            radios.addAll(nodeService.radios().values());

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Inputs", routes.InputsController.index());

            return ok(views.html.system.inputs.index.render(
                    currentUser(),
                    bc,
                    globalInputs,
                    localInputs,
                    nodes,
                    radios,
                    inputService.getAllInputTypeInformation(),
                    nodeService.loadMasterNode()
            ));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    public Result manage(String nodeId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_READ)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Map<Input, Map<ClusterEntity, InputState>> globalInputs = Maps.newHashMap();
        final List<InputState> localInputs = Lists.newArrayList();

        try {
            Node node = nodeService.loadNode(nodeId);

            if (node == null) {
                String message = "Did not find node.";
                return status(404, views.html.errors.error.render(message, new RuntimeException(), request()));
            }

            for (InputState inputState : inputService.loadAllInputStates(node)) {
                if (inputState.getInput().getGlobal() == false)
                    localInputs.add(inputState);
                else {
                    Map<ClusterEntity, InputState> clusterEntityInputStateMap = Maps.newHashMap();
                    clusterEntityInputStateMap.put(node, inputState);
                    globalInputs.put(inputState.getInput(), clusterEntityInputStateMap);
                }
            }

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(node.getShortNodeId(), routes.NodesController.node(node.getNodeId()));
            bc.addCrumb("Inputs", routes.InputsController.manage(node.getNodeId()));

            return ok(views.html.system.inputs.manage.render(
                    currentUser(),
                    bc,
                    node,
                    globalInputs,
                    localInputs,
                    node.getAllInputTypeInformation()
            ));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result manageRadio(String radioId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_READ)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Map<Input, Map<ClusterEntity, InputState>> globalInputs = Maps.newHashMap();
        final List<InputState> localInputs = Lists.newArrayList();

        try {
            Radio radio = nodeService.loadRadio(radioId);

            if (radio == null) {
                String message = "Did not find radio.";
                return status(404, views.html.errors.error.render(message, new RuntimeException(), request()));
            }

            for (InputState inputState : inputService.loadAllInputStates(radio)) {
                if (!inputState.getInput().getGlobal())
                    localInputs.add(inputState);
                else {
                    Map<ClusterEntity, InputState> clusterEntityInputStateMap = Maps.newHashMap();
                    clusterEntityInputStateMap.put(radio, inputState);
                    globalInputs.put(inputState.getInput(), clusterEntityInputStateMap);
                }
            }

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(radio.getShortNodeId(), routes.RadiosController.show(radio.getId()));
            bc.addCrumb("Inputs", routes.InputsController.manageRadio(radio.getId()));

            return ok(views.html.system.inputs.manage_radio.render(
                    currentUser(),
                    bc,
                    radio,
                    globalInputs,
                    localInputs,
                    radio.getAllInputTypeInformation(),
                    nodeService.loadMasterNode()
            ));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    protected Map<String, Object> extractConfiguration(Map<String, Object> form, InputTypeSummaryResponse inputInfo)
            throws IllegalArgumentException {
        Map<String, Object> configuration = Maps.newHashMapWithExpectedSize(form.size());
        for (final Map.Entry<String, Object> entry : form.entrySet()) {
            final Object value;
            // Decide what to cast to. (string, bool, number)
            switch ((String) inputInfo.requestedConfiguration.get(entry.getKey()).get("type")) {
                case "text":
                    value = String.valueOf(entry.getValue());
                    break;
                case "number":
                    try {
                        value = Integer.parseInt(String.valueOf(entry.getValue()));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(e);
                    }
                    break;
                case "boolean":
                    value = "true".equals(String.valueOf(entry.getValue()));
                    break;
                case "dropdown":
                    value = String.valueOf(entry.getValue());
                    break;
                default:
                    value = entry.getValue();
            }

            configuration.put(entry.getKey(), value);
        }

        return configuration;
    }

    public Result launch() {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_EDIT)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Form<LaunchInputRequest> form = launchInputRequestForm.bindFromRequest();
        final LaunchInputRequest request = form.get();
        final String nodeIdParam = request.node;

        try {
            final InputTypeSummaryResponse inputInfo = getInputInfo(nodeIdParam, request);

            final Map<String, Object> configuration;

            try {
                configuration = extractConfiguration(request.configuration, inputInfo);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid configuration for input " + request.title, e);
            }

            try {
                final ClusterEntity node = getClusterEntity(nodeIdParam, request);
                final Boolean result;
                if (request.global) {
                    result = (inputService.launchGlobal(request.title, request.type, configuration, inputInfo.isExclusive) != null);
                } else {
                    final InputLaunchResponse response = nodeService.loadMasterNode().launchInput(request.title, request.type, request.global, configuration, inputInfo.isExclusive, nodeIdParam);
                    result = node.launchExistingInput(response.id);
                }

                if (!result) {
                    return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, new RuntimeException("Could not launch input " + request.title), request()));
                }
            } catch (ExclusiveInputException e) {
                flash("error", "This input is exclusive and already running.");
                return redirect(routes.InputsController.index());
            }

            return redirect(routes.InputsController.index());
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not launch input. We expected HTTP 202, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    private InputTypeSummaryResponse getInputInfo(String nodeIdParam, LaunchInputRequest request) throws NodeService.NodeNotFoundException, APIException, IOException {
        final ClusterEntity node = getClusterEntity(nodeIdParam, request);

        if (node == null)
            throw new RuntimeException("Could not find Node to launch input on!");

        return node.getInputTypeInformation(request.type);
    }

    private ClusterEntity getClusterEntity(String nodeIdParam, LaunchInputRequest request) throws NodeService.NodeNotFoundException, APIException, IOException {
        ClusterEntity node = null;
        String nodeId = null;
        if (nodeIdParam != null && !nodeIdParam.isEmpty()) {
            nodeId = nodeIdParam;
        } else {
            if (request.node != null && !request.node.isEmpty()) {
                nodeId = request.node;
            }
        }

        if (request.global)
            node = nodeService.loadMasterNode();
        else if (nodeId != null) {
            try {
                node = nodeService.loadNode(nodeId);
            } catch (NodeService.NodeNotFoundException e) {
                node = nodeService.loadRadio(nodeId);
            }
        }

        return node;
    }

    public Result update(String inputId) throws APIException, NodeService.NodeNotFoundException, IOException {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_EDIT)) {
            return redirect(routes.StartpageController.redirect());
        }

        final Form<LaunchInputRequest> form = launchInputRequestForm.bindFromRequest();
        final LaunchInputRequest request = form.get();

        final String nodeIdParam;
        if (request.global)
            nodeIdParam = nodeService.loadMasterNode().getNodeId();
        else
            nodeIdParam = request.node;

        final InputTypeSummaryResponse inputInfo = getInputInfo(nodeIdParam, request);

        final Map<String, Object> configuration;

        try {
            configuration = extractConfiguration(request.configuration, inputInfo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid configuration for input " + request.title, e);
        }

        try {
            final Node node = nodeService.loadMasterNode();
            final InputLaunchResponse response = node.updateInput(inputId, request.title, request.type, request.global, configuration, request.node);
            inputService.restart(inputId);
        } catch (APIException | IOException e) {
            e.printStackTrace();
        }

        return redirect(routes.InputsController.index());
    }

    public Result terminate(String nodeId, String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_TERMINATE)) {
            return redirect(routes.StartpageController.redirect());
        }

        try {
            if (!nodeService.loadNode(nodeId).terminateInput(inputId)) {
                flash("error", "Could not terminate input " + inputId);
            }
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }

        return redirect(routes.InputsController.index());
    }

    public Result terminateRadio(String radioId, String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_TERMINATE)) {
            return redirect(routes.StartpageController.redirect());
        }
        try {
            if (!nodeService.loadRadio(radioId).terminateInput(inputId)) {
                flash("error", "Could not terminate input " + inputId);
            }
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }

        return redirect(routes.InputsController.index());
    }

    public Result terminateGlobal(String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_TERMINATE)) {
            return redirect(routes.StartpageController.redirect());
        }
        Map<ClusterEntity, Boolean> results = inputService.terminateGlobal(inputId);

        if (results.values().contains(false)) {
            List<ClusterEntity> failingNodes = Lists.newArrayList();

            for (Map.Entry<ClusterEntity, Boolean> entry : results.entrySet())
                if (!entry.getValue())
                    failingNodes.add(entry.getKey());

            flash("error", "Could not terminate input on nodes " + Joiner.on(", ").join(failingNodes));
        }

        return redirect(routes.InputsController.index());

    }

    public Result stop(String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_STOP)) {
            flash("error", "You are not permitted to stop this input.");
            return redirect(routes.InputsController.index());
        }

        try {
            inputService.stop(inputId);
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }

        return redirect(routes.InputsController.index());
    }

    public Result start(String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_START)) {
            flash("error", "You are not permitted to stop this input.");
            return redirect(routes.InputsController.index());
        }

        try {
            inputService.start(inputId);
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }

        return redirect(routes.InputsController.index());
    }

    public Result restart(String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_START) && !Permissions.isPermitted(RestPermissions.INPUTS_STOP)) {
            flash("error", "You are not permitted to stop this input.");
            return redirect(routes.InputsController.index());
        }

        try {
            inputService.restart(inputId);
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }

        return redirect(routes.InputsController.index());
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
    public Result addStaticField(String nodeId, String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_EDIT, inputId)) {
            return redirect(routes.StartpageController.redirect());
        }

        Map<String, String[]> form = request().body().asFormUrlEncoded();

        if (form.get("key") == null || form.get("value") == null) {
            flash("error", "Missing parameters.");
            return redirect(routes.InputsController.index());
        }

        String key = form.get("key")[0];
        String value = form.get("value")[0];

        try {
            nodeService.loadNode(nodeId).getInput(inputId).addStaticField(key, value);
            return redirect(routes.InputsController.index());
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not add static field. We expected HTTP 202, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result addStaticFieldGlobal(String inputId) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_EDIT, inputId)) {
            return redirect(routes.StartpageController.redirect());
        }

        return addStaticField(servernodes.master().getNodeId(), inputId);
    }

    public Result removeStaticField(String nodeId, String inputId, String key) {
        if (!Permissions.isPermitted(RestPermissions.INPUTS_EDIT, inputId)) {
            return redirect(routes.StartpageController.redirect());
        }

        try {
            Node node;
            if (nodeId != null && !nodeId.isEmpty())
                node = nodeService.loadNode(nodeId);
            else
                node = nodeService.loadMasterNode();

            node.getInput(inputId).removeStaticField(key);

            return redirect(routes.InputsController.index());
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not delete static field. We expected HTTP 204, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }
}
