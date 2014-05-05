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

package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.lib.metrics.Gauge;
import org.graylog2.restclient.lib.metrics.Metric;
import org.graylog2.restclient.models.api.requests.MultiMetricRequest;
import org.graylog2.restclient.models.api.requests.SystemJobTriggerRequest;
import org.graylog2.restclient.models.api.responses.metrics.MetricsListResponse;
import org.graylog2.restclient.models.api.responses.system.*;
import org.graylog2.restclient.models.api.responses.system.indices.IndexerFailureCountResponse;
import org.graylog2.restclient.models.api.responses.system.indices.IndexerFailuresResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClusterService {
    private static final Logger log = LoggerFactory.getLogger(ClusterService.class);
    private final ApiClient api;
    private final SystemJob.Factory systemJobFactory;
    private final ServerNodes serverNodes;
    private final NodeService nodeService;

    @Inject
    private ClusterService(ApiClient api,
                           SystemJob.Factory systemJobFactory,
                           ServerNodes serverNodes,
                           NodeService nodeService) {
        this.api = api;
        this.systemJobFactory = systemJobFactory;
        this.serverNodes = serverNodes;
        this.nodeService = nodeService;
    }

    public void triggerSystemJob(SystemJob.Type type, User user) throws IOException, APIException {
        api.post()
                .path("/system/jobs")
                .body(new SystemJobTriggerRequest(type, user))
                .expect(Http.Status.ACCEPTED)
                .execute();
    }

    public List<Notification> allNotifications() throws IOException, APIException {
        GetNotificationsResponse r = api.get(GetNotificationsResponse.class).path("/system/notifications").execute();

        List<Notification> notifications = Lists.newArrayList();
        for (NotificationSummaryResponse notification : r.notifications) {
            try {
                notifications.add(new Notification(notification));
            } catch(IllegalArgumentException e) {
                play.Logger.warn("There is a notification type we can't handle: [" + notification.type + "]");
                continue;
            }
        }

        return notifications;
    }

    public void deleteNotification(Notification.Type type) throws APIException, IOException {
        api.delete()
                .path("/system/notifications/{0}", type.toString().toLowerCase())
                .expect(204)
                .execute();
    }

    public long getIndexerFailureCountLast24Hours() throws APIException, IOException {
        IndexerFailureCountResponse r = api.get(IndexerFailureCountResponse.class)
                .path("/system/indexer/failures/count")
                .queryParam("since", ISODateTimeFormat.dateTime().print(new DateTime(DateTimeZone.UTC).minusDays(1)))
                .execute();

        return r.count;
    }

    public IndexerFailuresResponse getIndexerFailures(int limit, int offset) throws APIException, IOException {
        return api.get(IndexerFailuresResponse.class)
                .path("/system/indexer/failures")
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .execute();
    }

    public List<SystemMessage> getSystemMessages(int page) throws IOException, APIException {
        GetSystemMessagesResponse r = api.get(GetSystemMessagesResponse.class)
                .path("/system/messages")
                .queryParam("page", page)
                .execute();
        List<SystemMessage> messages = Lists.newArrayList();
        for (SystemMessageSummaryResponse message : r.messages) {
            messages.add(new SystemMessage(message));
        }

        return messages;
    }

    public int getNumberOfSystemMessages() throws IOException, APIException {
        return api.get(GetSystemMessagesResponse.class).path("/system/messages").execute().total;
    }

    public List<SystemJob> allSystemJobs() throws IOException, APIException {
        List<SystemJob> jobs = Lists.newArrayList();

        for(Node node : serverNodes.all()) {
            GetSystemJobsResponse r = api.get(GetSystemJobsResponse.class).node(node).path("/system/jobs").execute();

            for (SystemJobSummaryResponse job : r.jobs) {
                jobs.add(systemJobFactory.fromSummaryResponse(job));
            }
        }

        return jobs;
    }

    public ESClusterHealth getESClusterHealth() {
        try {
            final ESClusterHealthResponse response = api.get(ESClusterHealthResponse.class).path("/system/indexer/cluster/health").execute();
            return new ESClusterHealth(response);
        } catch (APIException e) {
            log.error("Could not load es cluster health", e);
        } catch (IOException e) {
            log.error("Could not load es cluster health", e);
        }
        return null;
    }

    public List<NodeJVMStats> getClusterJvmStats() {
        List<NodeJVMStats> result = Lists.newArrayList();
        Map<Node, ClusterEntityJVMStatsResponse> rs = api.get(ClusterEntityJVMStatsResponse.class).fromAllNodes().path("/system/jvm").executeOnAll();

        for (Map.Entry<Node, ClusterEntityJVMStatsResponse> entry : rs.entrySet()) {
            if (entry.getValue() == null) {
                log.warn("Skipping failed jvm stats request for node {}", entry.getKey());
                continue;
            }
            result.add(new NodeJVMStats(entry.getValue()));
        }

        return result;
    }

    public F.Tuple<Integer, Integer> getClusterThroughput() {
        final Map<Node, NodeThroughputResponse> responses =
                api.get(NodeThroughputResponse.class).fromAllNodes().path("/system/throughput").executeOnAll();
        int t = 0;
        for (Map.Entry<Node, NodeThroughputResponse> entry : responses.entrySet()) {
            if (entry.getValue() == null) {
                log.warn("Skipping failed throughput request for node {}", entry.getKey());
                continue;
            }
            t += entry.getValue().throughput;
        }
        return F.Tuple(t, responses.size());
    }


    // TODO duplicated
    private long asLong(String read_bytes, Map<String, Metric> metrics) {
        return ((Double) ((Gauge) metrics.get(read_bytes)).getValue()).longValue();
    }

    // TODO duplicated
    private String buildNetworkIOMetricName(String base, boolean total) {
        StringBuilder metricName = new StringBuilder(base).append("_");

        if (total) {
            metricName.append("total");
        } else {
            metricName.append("1sec");
        }

        return metricName.toString();
    }

    // TODO duplicated
    private String qualifiedIOMetricName(String type, String id, String base, boolean total) {
        return type + "." + id + "." + buildNetworkIOMetricName(base, total);
    }

    public Input.IoStats getGlobalInputIo(Input input) {
        final Input.IoStats ioStats = new Input.IoStats();

        final String inputId = input.getId();
        final String type = input.getType();

        try {
            MultiMetricRequest request = new MultiMetricRequest();
            final String read_bytes = qualifiedIOMetricName(type, inputId, "read_bytes", false);
            final String read_bytes_total = qualifiedIOMetricName(type, inputId, "read_bytes", true);
            final String written_bytes = qualifiedIOMetricName(type, inputId, "written_bytes", false);
            final String written_bytes_total = qualifiedIOMetricName(type, inputId, "written_bytes", true);
            request.metrics = new String[]{read_bytes, read_bytes_total, written_bytes, written_bytes_total};

            final Map<Node, MetricsListResponse> results = api.post(MetricsListResponse.class)
                    .body(request)
                    .path("/system/metrics/multiple")
                    .expect(200, 404)
                    .executeOnAll();

            for (MetricsListResponse response : results.values()) {
                final Map<String, Metric> metrics = response.getMetrics();

                ioStats.readBytes += asLong(read_bytes, metrics);
                ioStats.readBytesTotal += asLong(read_bytes_total, metrics);
                ioStats.writtenBytes += asLong(written_bytes, metrics);
                ioStats.writtenBytesTotal += asLong(written_bytes_total, metrics);
            }

            for (Radio radio : nodeService.radios().values()) {
                try {
                    final MetricsListResponse radioResponse = api.post(MetricsListResponse.class)
                            .body(request)
                            .radio(radio)
                            .path("/system/metrics/multiple")
                            .expect(200, 404)
                            .execute();
                    final Map<String, Metric> metrics = radioResponse.getMetrics();

                    ioStats.readBytes += asLong(read_bytes, metrics);
                    ioStats.readBytesTotal += asLong(read_bytes_total, metrics);
                    ioStats.writtenBytes += asLong(written_bytes, metrics);
                    ioStats.writtenBytesTotal += asLong(written_bytes_total, metrics);
                } catch (IOException e) {
                    log.error("Unable to load metrics for radio node {}", radio.getId());
                } catch (APIException e) {
                    log.error("Unable to load metrics for radio node", radio.getId());
                }
            }

        } catch (IOException e) {
            log.error("Unable to load master node", e);
        } catch (APIException e) {
            log.error("Unable to load master node", e);
        }
        return ioStats;
    }
}
