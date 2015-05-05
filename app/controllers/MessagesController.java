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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.results.MessageAnalyzeResult;
import org.graylog2.restclient.models.api.results.MessageResult;
import play.Logger;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static lib.security.RestPermissions.INPUTS_READ;
import static lib.security.RestPermissions.STREAMS_READ;
import static views.helpers.Permissions.isPermitted;

public class MessagesController extends AuthenticatedController {

    private final NodeService nodeService;
    private final StreamService streamService;
    private final MessagesService messagesService;

    @Inject
    public MessagesController(NodeService nodeService, StreamService streamService, MessagesService messagesService) {
        this.nodeService = nodeService;
        this.streamService = streamService;
        this.messagesService = messagesService;
    }

    public Result partial(String index, String id) {
        try {
            MessageResult message = messagesService.getMessage(index, id);
            Node sourceNode = getSourceNode(message);
            Radio sourceRadio = getSourceRadio(message);
            List<Stream> messageInStreams = Lists.newArrayList();

            for (String streamId : message.getStreamIds()) {
                if (isPermitted(STREAMS_READ, streamId)) {
                    try {
                        messageInStreams.add(streamService.get(streamId));
                    } catch (APIException e) {
                        //  We get a 404 if the stream no longer exists.
                        Logger.debug("Skipping stream of message", e);
                    }
                }
            }

            return ok(views.html.messages.show_as_partial.render(
                            message,
                            messageInStreams,
                            getSourceInput(sourceNode, message),
                            sourceNode,
                            sourceRadio,
                            getSourceInput(sourceRadio, message),
                            streamService.all())
            );
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    // TODO move this to an API controller.
    public Result single(String index, String id) {
        return single(index, id, false);
    }

    public Result single(String index, String id, Boolean filtered) {
        try {

            MessageResult message = messagesService.getMessage(index, id);

            Map<String, Object> result = Maps.newHashMap();
            result.put("id", message.getId());
            result.put("index", message.getIndex());
            if (filtered)
                result.put("fields", message.getFilteredFields());
            else
                result.put("fields", message.getFields());

            return ok(Json.toJson(result));
        } catch (IOException e) {
            return status(500);
        } catch (APIException e) {
            return status(e.getHttpCode());
        }
    }

    public Result analyze(String index, String id, String field) {
        try {
            MessageResult message = messagesService.getMessage(index, id);

            Object analyzeField = message.getFilteredFields().get(field);
            if (analyzeField == null || (analyzeField instanceof String) && ((String)analyzeField).isEmpty()) {
                return status(404, "Message does not have requested field " + field);
            }
            final String stringifiedValue = String.valueOf(analyzeField);
            MessageAnalyzeResult result = messagesService.analyze(index, stringifiedValue);
            return ok(Json.toJson(result.getTokens()));
        } catch (IOException e) {
            return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(500, views.html.errors.error.render(message, e, request()));
        }
    }

    private Node getSourceNode(MessageResult m) {
        try {
            return nodeService.loadNode(m.getSourceNodeId());
        } catch (Exception e) {
            Logger.warn("Could not derive source node from message <" + m.getId() + ">.", e);
        }

        return null;
    }


    private Radio getSourceRadio(MessageResult m) {
        if (m.viaRadio()) {
            try {
                return nodeService.loadRadio(m.getSourceRadioId());
            } catch (Exception e) {
                Logger.warn("Could not derive source radio from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Node node, MessageResult m) {
        if (node != null && isPermitted(INPUTS_READ, m.getSourceInputId())) {
            try {
                return node.getInput(m.getSourceInputId());
            } catch (Exception e) {
                Logger.warn("Could not derive source input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

    private static Input getSourceInput(Radio radio, MessageResult m) {
        if (radio != null) {
            try {
                return radio.getInput(m.getSourceRadioInputId());
            } catch (Exception e) {
                Logger.warn("Could not derive source radio input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }

}
