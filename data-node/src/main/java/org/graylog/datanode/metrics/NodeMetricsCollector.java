/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class NodeMetricsCollector {

    private static final long CGROUP_UNLIMITED_THRESHOLD = 1L << 60;

    private final OfficialOpensearchClient client;
    private final ObjectMapper objectMapper;
    private final Map<String, CpuSample> lastCpuSamples = new ConcurrentHashMap<>();
    Logger log = LoggerFactory.getLogger(NodeMetricsCollector.class);

    private static class CpuSample {
        final long timestampMillis;
        final long usageNanos;

        CpuSample(long timestampMillis, long usageNanos) {
            this.timestampMillis = timestampMillis;
            this.usageNanos = usageNanos;
        }
    }

    public NodeMetricsCollector(OfficialOpensearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public OfficialOpensearchClient getClient() {
        return client;
    }

    /**
     * Returns the raw, unconverted OpenSearch node stats keyed by {@link NodeStatMetrics#getFieldName()}.
     * Byte metrics are kept in bytes here so that the metric-registry gauges (served by the
     * {@code /rest/metrics/multiple} endpoint) stay consistent with their "*_in_bytes" names. Unit conversion
     * to GiB/MiB for the metrics index and its dashboards is applied by the caller via
     * {@link NodeStatMetrics#mapValue(String, Object)}.
     */
    public Map<String, Object> getNodeMetrics(String node) {
        Map<String, Object> metrics = new HashMap<>();

        Request nodeStatRequest = Requests.builder()
                .method("GET")
                .endpoint("_nodes/" + node + "/stats")
                .build();
        final DocumentContext nodeContext = getNodeContextFromRequest(node, nodeStatRequest);

        if (Objects.nonNull(nodeContext)) {
            Arrays.stream(NodeStatMetrics.values())
                    .filter(m -> Objects.nonNull(m.getNodeStat()))
                    .forEach(metric -> {
                        try {
                            metrics.put(metric.getFieldName(), nodeContext.read(metric.getNodeStat()));
                        } catch (Exception e) {
                            log.error("Could not retrieve metric {} for node {}", metric.getFieldName(), node);
                        }
                    });
            applyCgroupMemoryMetrics(nodeContext, metrics);
            applyCgroupCpuMetrics(nodeContext, metrics, node);
        }

        return metrics;
    }

    private void applyCgroupMemoryMetrics(DocumentContext nodeContext, Map<String, Object> metrics) {
        try {
            final Object rawLimit = nodeContext.read("$.os.cgroup.memory.limit_in_bytes");
            final Object rawUsage = nodeContext.read("$.os.cgroup.memory.usage_in_bytes");
            final long limitBytes = parseCgroupLong(rawLimit);
            final long usedBytes = parseCgroupLong(rawUsage);
            if (limitBytes > 0 && limitBytes < CGROUP_UNLIMITED_THRESHOLD && usedBytes >= 0) {
                final long freeBytes = Math.max(0L, limitBytes - usedBytes);
                final int usedPercent = (int) Math.min(100L, Math.max(0L, Math.round((double) usedBytes / limitBytes * 100.0)));
                metrics.put(NodeStatMetrics.MEM_TOTAL.getFieldName(), limitBytes);
                metrics.put(NodeStatMetrics.MEM_TOTAL_USED_BYTES.getFieldName(), usedBytes);
                metrics.put(NodeStatMetrics.MEM_FREE.getFieldName(), freeBytes);
                metrics.put(NodeStatMetrics.MEM_TOTAL_USED.getFieldName(), usedPercent);
            }
        } catch (Exception e) {
            log.debug("Cgroup memory metrics not available: {}", e.getMessage());
        }
    }

    private void applyCgroupCpuMetrics(DocumentContext nodeContext, Map<String, Object> metrics, String node) {
        try {
            final Object rawUsageNanos = nodeContext.read("$.os.cgroup.cpuacct.usage_nanos");
            final Object rawTimestamp = nodeContext.read("$.os.timestamp");
            if (rawUsageNanos == null || rawTimestamp == null) {
                return;
            }
            final long usageNanos = parseCgroupLong(rawUsageNanos);
            final long timestampMillis = parseCgroupLong(rawTimestamp);
            if (usageNanos <= 0 || timestampMillis <= 0) {
                return;
            }

            final CpuSample prev = lastCpuSamples.get(node);
            lastCpuSamples.put(node, new CpuSample(timestampMillis, usageNanos));

            if (prev == null) {
                return;
            }

            final long deltaUsageNanos = usageNanos - prev.usageNanos;
            final long deltaTimeNanos = (timestampMillis - prev.timestampMillis) * 1_000_000L;

            if (deltaUsageNanos >= 0 && deltaTimeNanos > 0) {
                final double effectiveCpus = determineEffectiveCpus(nodeContext);
                if (effectiveCpus > 0) {
                    final double cpuPercent = (double) deltaUsageNanos / (deltaTimeNanos * effectiveCpus) * 100.0;
                    final int roundedCpuPercent = (int) Math.min(100L, Math.max(0L, Math.round(cpuPercent)));
                    metrics.put(NodeStatMetrics.CPU_PERCENT.getFieldName(), roundedCpuPercent);
                }
            }
        } catch (Exception e) {
            log.debug("Cgroup CPU metrics not available for node {}: {}", node, e.getMessage());
        }
    }

    private double determineEffectiveCpus(DocumentContext nodeContext) {
        try {
            final Object rawQuota = nodeContext.read("$.os.cgroup.cpu.cfs_quota_micros");
            final Object rawPeriod = nodeContext.read("$.os.cgroup.cpu.cfs_period_micros");
            if (rawQuota != null && rawPeriod != null) {
                final long quota = parseCgroupLong(rawQuota);
                final long period = parseCgroupLong(rawPeriod);
                if (quota > 0 && period > 0) {
                    return (double) quota / period;
                }
            }
        } catch (Exception ignored) {
        }
        return Runtime.getRuntime().availableProcessors();
    }

    private long parseCgroupLong(Object value) {
        if (value == null) {
            return -1L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException e) {
                return -1L;
            }
        }
        return -1L;
    }

    private DocumentContext getNodeContextFromRequest(String node, Request nodeStatRequest) {
        JsonNode response = client.performRequest(nodeStatRequest, "Error retrieving node stats for node " + node);

        Filter nodeFilter = Filter.filter(Criteria.where("name").eq(node));
        Object nodeStatNode;
        try {
            nodeStatNode = JsonPath.read(objectMapper.writeValueAsString(response), "$['nodes'][*][?]", nodeFilter);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (nodeStatNode != null) {
            JsonNode nodeStats = objectMapper.convertValue(nodeStatNode, JsonNode.class);
            return JsonPath.parse(nodeStats.get(0).toString());
        }
        log.error("No node stats returned for node {}", node);
        return null;
    }

}
