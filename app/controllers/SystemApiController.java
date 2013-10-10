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
package controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import lib.APIException;
import models.*;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SystemApiController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private MessagesService messagesService;

    public Result fields() {
        Set<String> fields = messagesService.getMessageFields();

        Map<String, Set<String>> result = Maps.newHashMap();
        result.put("fields", fields);

        return ok(new Gson().toJson(result)).as("application/json");
    }

    public Result jobs() {
        try {
            List<Map<String, Object>> jobs = Lists.newArrayList();
            for (SystemJob j : clusterService.allSystemJobs()) {
                Map<String, Object> job = Maps.newHashMap();

                job.put("id", j.getId());
                job.put("percent_complete", j.getPercentComplete());

                jobs.add(job);
            }

            Map<String, Object> result = Maps.newHashMap();
            result.put("jobs", jobs);

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result notifications() {
        try {
            Map<String, Object> result = Maps.newHashMap();
            result.put("count", clusterService.allNotifications().size());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result deleteNotification(String notificationType) {
        try {
            clusterService.deleteNotification(Notification.Type.fromString(notificationType));
            return ok();
        } catch (IllegalArgumentException e1) {
            return notFound("no such notification type");
        } catch (APIException e2) {
            return internalServerError("api exception " + e2);
        } catch (IOException e3) {
            return internalServerError("io exception");
        }
    }

    public Result totalThroughput() {
        Map<String, Object> result = Maps.newHashMap();
        final F.Tuple<Integer, Integer> throughputPerNodes = clusterService.getClusterThroughput();
        result.put("throughput", throughputPerNodes._1);
        result.put("nodecount", throughputPerNodes._2);

        return ok(new Gson().toJson(result)).as("application/json");
    }

    public Result nodeThroughput(String nodeId) {
        Map<String, Object> result = Maps.newHashMap();
        final Node node = nodeService.loadNode(nodeId);
        int throughput = node.getThroughput();
        result.put("throughput", throughput);

        return ok(new Gson().toJson(result)).as("application/json");
    }

    public Result pauseMessageProcessing() {
        try {
            Http.RequestBody body = request().body();
            final String nodeId = body.asFormUrlEncoded().get("node_id")[0];
            final Node node = nodeService.loadNode(nodeId);
            node.pause();
            return ok();
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result resumeMessageProcessing() {
        try {
            Http.RequestBody body = request().body();
            final String nodeId = body.asFormUrlEncoded().get("node_id")[0];
            final Node node = nodeService.loadNode(nodeId);
            node.resume();
            return ok();
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }
}
