/*
 * Copyright 2013 TORCH UG
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
 */
package models;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import lib.timeranges.InvalidRangeParametersException;
import lib.timeranges.RelativeRange;
import models.api.responses.MessageSummaryResponse;
import models.api.responses.system.InputSummaryResponse;
import models.api.results.MessageResult;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input {
    public interface Factory {
        Input fromSummaryResponse(InputSummaryResponse input);
    }

    private final ApiClient api;
    private final UniversalSearch.Factory searchFactory;
    private final String type;
    private final String id;
    private final String persistId;
    private final String name;
    private final String title;
    private final User creatorUser;
    private final DateTime startedAt;
    private final Map<String, Object> attributes;

    @AssistedInject
    private Input(ApiClient api, UniversalSearch.Factory searchFactory, @Assisted InputSummaryResponse is) {
        this.api = api;
        this.searchFactory = searchFactory;
        this.type = is.type;
        this.id = is.inputId;
        this.persistId = is.persistId;
        this.name = is.name;
        this.title = is.title;
        this.startedAt = DateTime.parse(is.startedAt);
        this.creatorUser = User.load(is.creatorUserId);
        this.attributes = is.attributes;

        // We might get a double parsed from JSON here. Make sure to round it to Integer. (would be .0 anyways)
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getValue() instanceof Double) {
                attributes.put(e.getKey(), Math.round((Double) e.getValue()));
            }
        }
    }

    public void terminate(Node node) throws IOException, APIException {
        node.terminateInput(id);
    }

    public MessageResult getRecentlyReceivedMessage(String nodeId) throws IOException, APIException {
        String query = "gl2_source_node:" + nodeId + " AND gl2_source_input:" + id;

        UniversalSearch search = null;
        try {
            search = searchFactory.queryWithRange(query, new RelativeRange(60 * 60 * 24));
        } catch (InvalidRangeParametersException e) {
            return null; // cannot happen(tm)
        }
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
