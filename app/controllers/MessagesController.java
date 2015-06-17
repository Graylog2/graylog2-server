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

import com.google.common.collect.Maps;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.api.results.MessageAnalyzeResult;
import org.graylog2.restclient.models.api.results.MessageResult;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

public class MessagesController extends AuthenticatedController {

    private final MessagesService messagesService;

    @Inject
    public MessagesController(MessagesService messagesService) {
        this.messagesService = messagesService;
    }

    public Result single(String index, String id) {
        try {

            final MessageResult message = messagesService.getMessage(index, id);

            Map<String, Object> result = Maps.newHashMap();
            result.put("id", message.getId());
            result.put("index", message.getIndex());
            result.put("filtered_fields", message.getFilteredFields());
            result.put("fields", message.getFields());
            result.put("formatted_fields", message.getFormattedFields());

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
}
