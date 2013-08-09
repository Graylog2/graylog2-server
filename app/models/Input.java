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
import lib.ExclusiveInputException;
import models.api.requests.InputLaunchRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.MessageSummaryResponse;
import models.api.responses.system.InputSummaryResponse;
import models.api.responses.system.InputTypeSummaryResponse;
import models.api.responses.system.InputTypesResponse;
import models.api.results.MessageResult;
import org.joda.time.DateTime;
import play.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input {

    private final String type;
    private final String id;
    private final String persistId;
    private final String name;
    private final String title;
    private final User creatorUser;
    private final DateTime startedAt;
    private final Map<String, Object> attributes;

    public Input(InputSummaryResponse is) {
        this(
                is.type,
                is.inputId,
                is.persistId,
                is.name,
                is.title,
                is.startedAt,
                User.load(is.creatorUserId),
                is.attributes
        );
    }

    public Input(String type, String id, String persistId, String name, String title, String startedAt, User creatorUser, Map<String, Object> attributes) {
        this.type = type;
        this.id = id;
        this.persistId = persistId;
        this.name = name;
        this.title = title;
        this.startedAt = DateTime.parse(startedAt);
        this.creatorUser = creatorUser;
        this.attributes = attributes;

        // We might get a double parsed from JSON here. Make sure to round it to Integer. (would be .0 anyways)
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getValue() instanceof Double) {
                attributes.put(e.getKey(), Math.round((Double) e.getValue()));
            }
        }
    }

    public static Map<String, String> getTypes(Node node) throws IOException, APIException {
        return Api.get(node, "system/inputs/types", InputTypesResponse.class).types;
    }

    public static InputTypeSummaryResponse getTypeInformation(Node node, String type) throws IOException, APIException {
        return Api.get(node, "system/inputs/types/" + type, InputTypeSummaryResponse.class);
    }

    public static Map<String, InputTypeSummaryResponse> getAllTypeInformation(Node node) throws IOException, APIException {
        Map<String, InputTypeSummaryResponse> types = Maps.newHashMap();

        List<InputTypeSummaryResponse> bools = Lists.newArrayList();
        for (String type : getTypes(node).keySet()) {
            InputTypeSummaryResponse itr = getTypeInformation(node, type);
            types.put(itr.type, itr);
        }

        return types;
    }

    public static void launch(Node node, String title, String type, Map<String, Object> configuration, String userId, boolean isExclusive) throws IOException, APIException, ExclusiveInputException {
        if (isExclusive) {
            for (Input input : node.getInputs()) {
                if(input.getType().equals(type)) {
                    throw new ExclusiveInputException();
                }
            }
        }

        InputLaunchRequest request = new InputLaunchRequest();
        request.title = title;
        request.type = type;
        request.configuration = configuration;
        request.creatorUserId = userId;

        Api.post(node, "system/inputs", request, 202, EmptyResponse.class);
    }

    public static void terminate(Node node, String inputId) throws IOException, APIException {
        Api.delete(node, "/system/inputs/" + inputId, 202, EmptyResponse.class);
    }

    public MessageResult getRecentlyReceivedMessage(String nodeId) throws IOException, APIException {
        String query = "gl2_source_node:" + nodeId + " AND gl2_source_input:" + id;

        UniversalSearch search = new UniversalSearch(query, 60*60*24);
        List<MessageSummaryResponse> messages = search.search().getMessages();

        MessageSummaryResponse result;
        if (messages.size() > 0) {
            result = messages.get(0);
        } else {
            return null;
        }

        return new MessageResult(result.message, result.index);
    }

    public String getId() {
        return id;
    }

    public String getPersistId() {
        return persistId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public User getCreatorUser() {
        return creatorUser;
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
