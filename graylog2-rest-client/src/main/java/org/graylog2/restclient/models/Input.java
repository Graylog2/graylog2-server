/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models;

import com.google.common.base.Joiner;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.metrics.Gauge;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.lib.timeranges.InvalidRangeParametersException;
import org.graylog2.restclient.lib.timeranges.RelativeRange;
import org.graylog2.restclient.models.api.requests.AddStaticFieldRequest;
import org.graylog2.restclient.models.api.requests.MultiMetricRequest;
import org.graylog2.restclient.models.api.responses.metrics.GaugeResponse;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import org.graylog2.restclient.models.api.responses.system.InputSummaryResponse;
import org.graylog2.restclient.models.api.results.MessageResult;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Input {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Input.class);

    public interface Factory {
        Input fromSummaryResponse(InputSummaryResponse input, ClusterEntity node);
    }

    private final ApiClient api;
    private final UniversalSearch.Factory searchFactory;
    private final FieldMapper fieldMapper;
    private final ClusterEntity node;
    private final String type;
    private final String id;
    private final String persistId;
    private final String name;
    private final String title;
    private final DateTime createdAt;
    private final User creatorUser;
    private final Boolean global;
    private final Map<String, Object> attributes;
    private final Map<String, String> staticFields;

    @AssistedInject
    private Input(ApiClient api,
                  UniversalSearch.Factory searchFactory,
                  UserService userService,
                  FieldMapper fieldMapper,
                  @Assisted InputSummaryResponse is,
                  @Assisted ClusterEntity node) {
        this.api = api;
        this.searchFactory = searchFactory;
        this.fieldMapper = fieldMapper;
        this.node = node;
        this.type = is.type;
        this.id = is.inputId;
        this.persistId = is.persistId;
        this.name = is.name;
        this.title = is.title;
        this.global = is.global;
        this.creatorUser = userService.load(is.creatorUserId);
        this.attributes = is.attributes;
        this.staticFields = is.staticFields;
        this.createdAt = DateTime.parse(is.createdAt);

        // We might get a double parsed from JSON here. Make sure to round it to Integer. (would be .0 anyways)
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (e.getValue() instanceof Double) {
                attributes.put(e.getKey(), Math.round((Double) e.getValue()));
            }
        }
    }

    public MessageResult getRecentlyReceivedMessage(String nodeId) throws IOException, APIException {
        String query = "gl2_source_node:" + nodeId + " AND gl2_source_input:" + id;

        UniversalSearch search;
        try {
            search = searchFactory.queryWithRange(query, new RelativeRange(60 * 60 * 24));
        } catch (InvalidRangeParametersException e) {
            return null; // cannot happen(tm)
        }
        List<MessageResult> messages = search.search().getMessages();

        MessageResult result;
        if (messages.size() > 0) {
            return messages.get(0);
        } else {
            return null;
        }
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

    public Map<String, String> getStaticFields() {
        return staticFields;
    }

    public void addStaticField(String key, String value) throws APIException, IOException {
        api.path(routes.StaticFieldsResource().create(id)).clusterEntity(node)
                .body(new AddStaticFieldRequest(key, value))
                .expect(Http.Status.CREATED)
                .execute();
    }

    public void removeStaticField(String key) throws APIException, IOException {
        api.path(routes.StaticFieldsResource().delete(key, id)).clusterEntity(node)
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

    public IoStats getIoStats() {
        MultiMetricRequest request = new MultiMetricRequest();
        final String read_bytes = qualifiedIOMetricName("read_bytes", false);
        final String read_bytes_total = qualifiedIOMetricName("read_bytes", true);
        final String written_bytes = qualifiedIOMetricName("written_bytes", false);
        final String written_bytes_total = qualifiedIOMetricName("written_bytes", true);
        request.metrics = new String[] { read_bytes, read_bytes_total, written_bytes, written_bytes_total };
        try {
            final MetricsListResponse response = api.path(routes.MetricsResource().multipleMetrics(), MetricsListResponse.class)
                    .clusterEntity(node)
                    .body(request)
                    .expect(200, 404)
                    .execute();

            final Map<String,Metric> metrics = response.getMetrics();
            final IoStats ioStats = new IoStats();
            // these are all Gauges, if this ever changes almost everything is broken...
            ioStats.readBytes = asLong(read_bytes, metrics);
            ioStats.readBytesTotal = asLong(read_bytes_total, metrics);
            ioStats.writtenBytes = asLong(written_bytes, metrics);
            ioStats.writtenBytesTotal = asLong(written_bytes_total, metrics);
            return ioStats;
        } catch (APIException e) {
            log.error("Unable to read IO metrics of input [{}]", this.id);
        } catch (IOException e) {
            log.error("Unable to read IO metrics of input [{}]", this.id);
        }
        log.debug("Returning empty iostats due to API error");
        return new IoStats();
    }

    private long asLong(String read_bytes, Map<String, Metric> metrics) {
        return ((Number)((Gauge)metrics.get(read_bytes)).getValue()).longValue();
    }

    private String qualifiedIOMetricName(String base, boolean total) {
        return type + "." + id + "." + buildNetworkIOMetricName(base, total);
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
            GaugeResponse response = api.path(routes.MetricsResource().singleMetric(Joiner.on(".").join(type, id, name)), GaugeResponse.class)
                .clusterEntity(node)
                .expect(200, 404)
                .execute();

            if (response == null || response.value == null) {
                return -1L;
            } else {
                return response.value;
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

    public Boolean getGlobal() {
        return global;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Input input = (Input) o;

        if (!persistId.equals(input.persistId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return persistId.hashCode();
    }

    public static class IoStats {
        public long readBytes;
        public long readBytesTotal;
        public long writtenBytes;
        public long writtenBytesTotal;
    }
}
