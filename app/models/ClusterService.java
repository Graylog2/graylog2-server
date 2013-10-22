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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.ServerNodes;
import models.api.requests.SystemJobTriggerRequest;
import models.api.responses.system.*;
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

    @Inject
    private ClusterService(ApiClient api, SystemJob.Factory systemJobFactory, ServerNodes serverNodes) {
        this.api = api;
        this.systemJobFactory = systemJobFactory;
        this.serverNodes = serverNodes;
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

    public List<ServerJVMStats> getClusterJvmStats() {
        List<ServerJVMStats> result = Lists.newArrayList();
        Map<Node, ServerJVMStatsResponse> rs = api.get(ServerJVMStatsResponse.class).fromAllNodes().path("/system/jvm").executeOnAll();

        for (ServerJVMStatsResponse r : rs.values()) {
            result.add(new ServerJVMStats(r));
        }

        return result;
    }

    public F.Tuple<Integer, Integer> getClusterThroughput() {
        final Map<Node, ServerThroughputResponse> responses =
                api.get(ServerThroughputResponse.class).fromAllNodes().path("/system/throughput").executeOnAll();
        int t = 0;
        for (ServerThroughputResponse r : responses.values()) {
            t += r.throughput;
        }
        return F.Tuple(t, responses.size());
    }
}
