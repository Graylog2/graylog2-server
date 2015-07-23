/**
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
 *
 */
package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.net.MediaType;
import controllers.AuthenticatedController;
import lib.json.Json;
import models.descriptions.InputDescription;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.models.ClusterService;
import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.InputService;
import org.graylog2.restclient.models.InputState;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.UniversalSearch;
import org.graylog2.restclient.models.api.results.MessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class InputsApiController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(InputsApiController.class);

    private final NodeService nodeService;
    private final ClusterService clusterService;
    private final InputService inputService;
    private final UniversalSearch.Factory searchFactory;

    @Inject
    public InputsApiController(NodeService nodeService,
                               ClusterService clusterService,
                               InputService inputService,
                               UniversalSearch.Factory searchFactory) {
        this.nodeService = nodeService;
        this.clusterService = clusterService;
        this.inputService = inputService;
        this.searchFactory = searchFactory;
    }

    public Result list() {
        final List<InputDescription> result = Lists.newArrayList();
        final List<InputState> inputStates = inputService.loadAllInputStates();

        for (InputState inputState : inputStates) {
            result.add(new InputDescription(inputState.getInput()));
        }

        return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result io(String nodeId, String inputId) {
        try {
            Map<String, Object> result = Maps.newHashMap();

            final Node node = nodeService.loadNode(nodeId);
            final Input input = node.getInput(inputId);
            final Input.IoStats ioStats = input.getIoStats();

            result.put("total_rx", Tools.byteToHuman(ioStats.readBytesTotal));
            result.put("total_tx", Tools.byteToHuman(ioStats.writtenBytesTotal));
            result.put("rx", Tools.byteToHuman(ioStats.readBytes));
            result.put("tx", Tools.byteToHuman(ioStats.writtenBytes));

            return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result globaIO(String inputId) {
        Map<String, Object> result = Maps.newHashMap();

        final Input globalInput;
        try {
            globalInput = nodeService.loadMasterNode().getInput(inputId);
            if (!globalInput.getGlobal()) {
                return badRequest();
            }
        } catch (IOException e) {
            log.error("Could not load input.");
            return internalServerError();
        } catch (APIException e) {
            log.error("Could not load input.");
            return internalServerError();
        }

        final Input.IoStats ioStats = clusterService.getGlobalInputIo(globalInput);

        result.put("total_rx", Tools.byteToHuman(ioStats.readBytesTotal));
        result.put("total_tx", Tools.byteToHuman(ioStats.writtenBytesTotal));
        result.put("rx", Tools.byteToHuman(ioStats.readBytes));
        result.put("tx", Tools.byteToHuman(ioStats.writtenBytes));

        return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
    }

    public Result connections(String nodeId, String inputId) {
        try {
            Map<String, Object> result = Maps.newHashMap();

            final Node node = nodeService.loadNode(nodeId);
            final Input input = node.getInput(inputId);

            result.put("active", input.getConnections());
            result.put("total", input.getTotalConnections());

            return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result globalRecentMessage(String inputId) throws InvalidRangeParametersException, IOException, APIException {
        final String query = "gl2_source_input:" + inputId + " OR gl2_source_radio_input:" + inputId;

        final UniversalSearch search = this.searchFactory.queryWithRange(query, new RelativeRange(86400));

        List<MessageResult> messages = search.search().getMessages();
        if (messages.size() > 0) {
            return ok(Json.toJsonString(buildResultFromMessage(messages.get(0)))).as(MediaType.JSON_UTF_8.toString());
        } else {
            return notFound();
        }
    }

    public Result recentMessage(String nodeId, String inputId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            MessageResult recentlyReceivedMessage = node.getInput(inputId).getRecentlyReceivedMessage(nodeId);

            if (recentlyReceivedMessage == null) {
                return notFound();
            }

            return ok(Json.toJsonString(buildResultFromMessage(recentlyReceivedMessage))).as(MediaType.JSON_UTF_8.toString());
        } catch (IOException e) {
            return status(500);
        } catch (APIException e) {
            return status(e.getHttpCode());
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    protected RecentMessageResult buildResultFromMessage(MessageResult message) {
        return new RecentMessageResult(message.getId(), message.getIndex(), message.getFields(), message.getFormattedFields(),
                message.getFilteredFields());
    }

    public static class RecentMessageResult {
        public final String id;
        public final String index;
        public final Map<String, Object> fields;
        public final Map<String, Object> formattedFields;
        public final Map<String, Object> filteredFields;

        public RecentMessageResult(String id, String index, Map<String, Object> fields,
                                   Map<String, Object> formattedFields, Map<String, Object> filteredFields) {
            this.id = id;
            this.index = index;
            this.fields = fields;
            this.formattedFields = formattedFields;
            this.filteredFields = filteredFields;
        }
    }
}
