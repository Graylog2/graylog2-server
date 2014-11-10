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
package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.metrics.Meter;
import org.graylog2.restclient.models.BufferInfo;
import org.graylog2.restclient.models.ClusterService;
import org.graylog2.restclient.models.MessagesService;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeJVMStats;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Notification;
import org.graylog2.restclient.models.Radio;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.SystemJob;
import play.libs.F;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SystemApiController extends AuthenticatedController {
    private final NodeService nodeService;
    private final ClusterService clusterService;
    private final MessagesService messagesService;
    private final StreamService streamService;

    @Inject
    public SystemApiController(NodeService nodeService, ClusterService clusterService, MessagesService messagesService, StreamService streamService) {
        this.nodeService = nodeService;
        this.clusterService = clusterService;
        this.messagesService = messagesService;
        this.streamService = streamService;
    }

    public Result fields() {
        Set<String> fields = messagesService.getMessageFields();

        Map<String, Set<String>> result = Maps.newHashMap();
        result.put("fields", fields);

        return ok(Json.toJson(result));
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

            return ok(Json.toJson(result));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result notifications() {
        try {
            /*Map<String, Object> result = Maps.newHashMap();
            result.put("count", clusterService.allNotifications().size());*/
            List<Notification> notifications = clusterService.allNotifications();

            return ok(Json.toJson(notifications));
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

        return ok(Json.toJson(result));
    }

    public Result nodeThroughput(String nodeId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            final Node node = nodeService.loadNode(nodeId);
            result.put("throughput", node.getThroughput());

            return ok(Json.toJson(result));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        }
    }

    public Result radioThroughput(String radioId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            final Radio radio = nodeService.loadRadio(radioId);
            result.put("throughput", radio.getThroughput());

            return ok(Json.toJson(result));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        }
    }

    public Result streamThroughput(String streamId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            final Stream stream = streamService.get(streamId);
            long throughput = stream.getThroughput();
            result.put("throughput", throughput);
            return ok(Json.toJson(result));
        } catch (APIException e) {
            return status(504, "Could not load stream " + streamId);
        } catch (IOException e) {
            return status(504, "Could not load stream " + streamId);
        }
    }

    public Result heap(String nodeId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            Node node = nodeService.loadNode(nodeId);

            return ok(Json.toJson(jvmMap(node.jvm(), node.getBufferInfo())));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        }
    }

    public Result radioHeap(String radioId) {
        try {
            Radio radio = nodeService.loadRadio(radioId);
            return ok(Json.toJson(jvmMap(radio.jvm(), radio.getBuffers())));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "radio not found");
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
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
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        }
    }

    @BodyParser.Of(BodyParser.FormUrlEncoded.class)
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
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        }
    }

    public Result internalLogsOfNode(String nodeId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            Node node = nodeService.loadNode(nodeId);
            Meter meter = (Meter) node.getSingleMetric("org.apache.log4j.Appender.all");
            result.put("total", meter.getTotal());

            return ok(Json.toJson(result));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    public Result internalLogMetricsOfNode(String nodeId) {
        try {
            Map<String, Object> result = Maps.newHashMap();
            Node node = nodeService.loadNode(nodeId);

            String[] levels = new String[]{
                    "org.apache.log4j.Appender.debug",
                    "org.apache.log4j.Appender.error",
                    "org.apache.log4j.Appender.fatal",
                    "org.apache.log4j.Appender.info",
                    "org.apache.log4j.Appender.trace",
                    "org.apache.log4j.Appender.warn"
            };

            for (String level : levels) {
                String shortName = level.substring(level.lastIndexOf(".") + 1);
                Map<String, Object> meterMap = Maps.newHashMap();

                Meter meter = (Meter) node.getSingleMetric(level);

                meterMap.put("total", meter.getTotal());
                meterMap.put("mean_rate", meter.getMeanFormatted());
                meterMap.put("one_min_rate", meter.getOneMinuteFormatted());

                result.put(shortName, meterMap);
            }

            return ok(Json.toJson(result));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, "node not found");
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }

    private Map<String, Object> jvmMap(NodeJVMStats jvm, BufferInfo bufferInfo) {
        Map<String, Object> result = Maps.newHashMap();

        result.put("free", jvm.getFreeMemory().getMegabytes());
        result.put("max", jvm.getMaxMemory().getMegabytes());
        result.put("total", jvm.getTotalMemory().getMegabytes());
        result.put("used", jvm.getUsedMemory().getMegabytes());
        result.put("used_percentage", jvm.usedMemoryPercentage());
        result.put("total_percentage", jvm.totalMemoryPercentage());
        result.put("input_master_cache", bufferInfo.getInputMasterCache().size);
        result.put("output_master_cache", bufferInfo.getOutputMasterCache().size);

        return result;
    }
}
