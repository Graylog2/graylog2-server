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
package controllers;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import lib.APIException;
import lib.Tools;
import models.Input;
import models.Node;
import models.NodeService;
import models.api.results.MessageResult;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class InputsApiController extends AuthenticatedController  {

    @Inject
    private NodeService nodeService;

    public Result io(String nodeId, String inputId) {
        try {
            Map<String, Object> result = Maps.newHashMap();

            final Node node = nodeService.loadNode(nodeId);
            final Input input = node.getInput(inputId);

            result.put("total_rx", Tools.byteToHuman(input.getTotalReadBytes()));
            result.put("total_tx", Tools.byteToHuman(input.getTotalWrittenBytes()));
            result.put("rx", Tools.byteToHuman(input.getReadBytes()));
            result.put("tx", Tools.byteToHuman(input.getWrittenBytes()));

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (
        APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result connections(String nodeId, String inputId) {
        try {
            Map<String, Object> result = Maps.newHashMap();

            final Node node = nodeService.loadNode(nodeId);
            final Input input = node.getInput(inputId);

            result.put("active", input.getConnections());
            result.put("total", input.getTotalConnections());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (
                APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result recentMessage(String nodeId, String inputId) {
        try {
            Node node = nodeService.loadNode(nodeId);
            MessageResult recentlyReceivedMessage = node.getInput(inputId).getRecentlyReceivedMessage(nodeId);

            if (recentlyReceivedMessage == null) {
                return notFound();
            }

            Map<String, Object> result = Maps.newHashMap();
            result.put("id", recentlyReceivedMessage.getId());
            result.put("fields", recentlyReceivedMessage.getFields());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return status(500);
        } catch (APIException e) {
            return status(e.getHttpCode());
        }
    }

}
