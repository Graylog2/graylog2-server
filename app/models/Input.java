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
import models.api.requests.AddStaticFieldRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.MessageSummaryResponse;
import models.api.responses.metrics.GaugeResponse;
import models.api.responses.system.InputSummaryResponse;
import models.api.results.MessageResult;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Input.class);

    public interface Factory {
        Input fromSummaryResponse(InputSummaryResponse input, Node node);
    }

    private final ApiClient api;
    private final UniversalSearch.Factory searchFactory;
    private final Node node;
    private final String type;
    private final String id;
    private final String persistId;
    private final String name;
    private final String title;
    private final User creatorUser;
    private final DateTime startedAt;
    private final Map<String, Object> attributes;
    private final Map<String, String> staticFields;

    @AssistedInject
    private Input(ApiClient api, UniversalSearch.Factory searchFactory, UserService userService, @Assisted InputSummaryResponse is, @Assisted Node node) {
        this.api = api;
        this.searchFactory = searchFactory;
        this.node = node;
        this.type = is.type;
        this.id = is.inputId;
        this.persistId = is.persistId;
        this.name = is.name;
        this.title = is.title;
        this.startedAt = DateTime.parse(is.startedAt);
        this.creatorUser = userService.load(is.creatorUserId);
        this.attributes = is.attributes;
        this.staticFields = is.staticFields;

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

    public Map<String, String> getStaticFields() {
        return staticFields;
    }

    public void addStaticField(String key, String value) throws APIException, IOException {
        api.post().node(node)
                .path("/system/inputs/{0}/staticfields", id)
                .body(new AddStaticFieldRequest(key, value))
                .expect(Http.Status.CREATED)
                .execute();
    }

    public void removeStaticField(String key) throws APIException, IOException {
        api.delete().node(node)
                .path("/system/inputs/{0}/staticfields/{1}", id, key)
                .expect(Http.Status.NO_CONTENT)
                .execute();
    }

    public long getConnections() {
        return getGaugeValue("open_connections");
    }

    public long getTotalConnections() {
        return getGaugeValue("total_connections");
    }

    public long getReadBytes() {
        return getGaugeValue(buildNetworkIOMetricName("read_bytes", false));
    }

    public long getWrittenBytes() {
        return getGaugeValue(buildNetworkIOMetricName("written_bytes", false));
    }

    public long getTotalReadBytes() {
        return getGaugeValue(buildNetworkIOMetricName("read_bytes", true));
    }

    public long getTotalWrittenBytes() {
        return getGaugeValue(buildNetworkIOMetricName("written_bytes", true));
    }

    private String buildNetworkIOMetricName(String base, boolean total) {
        StringBuilder metricName = new StringBuilder(base).append("_");

        if (total) {
            metricName.append("total");
        } else {
            metricName.append("1sec");
        }

        return metricName.toString();
    }

    private Long getGaugeValue(String name) {
        try {
            GaugeResponse response = api.get(GaugeResponse.class)
                .node(node)
                .path("/system/metrics/{0}.{1}.{2}", type, id, name)
                .expect(200, 404)
                .execute();

            if (response == null) {
                return -1L;
            } else {
                return (Long) response.value;
            }
        } catch (APIException e) {
            log.error("Unable to read throughput info of input [{}]", this.id, e);
        } catch (IOException e) {
            log.error("Unexpected exception", e);
        }

        return -1L;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
