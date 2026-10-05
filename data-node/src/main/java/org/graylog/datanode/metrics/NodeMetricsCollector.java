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
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.opensearch.client.opensearch.generic.Request;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.CgroupInfo;

import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class NodeMetricsCollector {

    private static final long CGROUP_UNLIMITED_THRESHOLD = 1L << 60;

    private final OfficialOpensearchClient client;
    private final ObjectMapper objectMapper;
    private final Supplier<CgroupInfo> cgroupInfo;
    private final Clock clock;
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
        this(client, objectMapper, Suppliers.memoize(NodeMetricsCollector::detectCgroupInfo), Clock.systemUTC());
    }

    NodeMetricsCollector(OfficialOpensearchClient client, ObjectMapper objectMapper, Supplier<CgroupInfo> cgroupInfo, Clock clock) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.cgroupInfo = cgroupInfo;
        this.clock = clock;
    }

    private static CgroupInfo detectCgroupInfo() {
        try {
            return new SystemInfo().getOperatingSystem().getCgroupInfo();
        } catch (LinkageError | RuntimeException e) {
            LoggerFactory.getLogger(NodeMetricsCollector.class).debug("Cgroup information not available: {}", e.getMessage());
            return null;
        }
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
            final CgroupInfo cgroup = cgroupInfo.get();
            if (cgroup != null) {
                applyCgroupMemoryMetrics(cgroup, metrics);
                applyCgroupCpuMetrics(cgroup, metrics, node);
            }
        }

        return metrics;
    }

    /**
     * OpenSearch's node stats only contain cgroup v1 information, so the cgroup is read directly. OpenSearch is
     * running in the same cgroup (container) as the Data Node. The used memory includes the page cache.
     */
    private void applyCgroupMemoryMetrics(CgroupInfo cgroup, Map<String, Object> metrics) {
        try {
            final long usedBytes = cgroup.getMemoryUsage();
            if (usedBytes <= 0) {
                return;
            }
            // Without a memory limit, the host memory is the effective limit, but the usage still has to be
            // the cgroup's and not the host's.
            final long limitBytes = cgroup.getMemoryLimit();
            final long totalBytes = (limitBytes > 0 && limitBytes < CGROUP_UNLIMITED_THRESHOLD)
                    ? limitBytes
                    : parseCgroupLong(metrics.get(NodeStatMetrics.MEM_TOTAL.getFieldName()));
            if (totalBytes <= 0) {
                return;
            }
            final long freeBytes = Math.max(0L, totalBytes - usedBytes);
            final int usedPercent = (int) Math.min(100L, Math.max(0L, Math.round((double) usedBytes / totalBytes * 100.0)));
            metrics.put(NodeStatMetrics.MEM_TOTAL.getFieldName(), totalBytes);
            metrics.put(NodeStatMetrics.MEM_TOTAL_USED_BYTES.getFieldName(), usedBytes);
            metrics.put(NodeStatMetrics.MEM_FREE.getFieldName(), freeBytes);
            metrics.put(NodeStatMetrics.MEM_TOTAL_USED.getFieldName(), usedPercent);
        } catch (RuntimeException e) {
            log.debug("Cgroup memory metrics not available: {}", e.getMessage());
        }
    }

    private void applyCgroupCpuMetrics(CgroupInfo cgroup, Map<String, Object> metrics, String node) {
        try {
            final long usageNanos = cgroup.getCpuUsage();
            final long timestampMillis = clock.millis();
            if (usageNanos <= 0) {
                return;
            }

            final CpuSample prev = lastCpuSamples.get(node);
            lastCpuSamples.put(node, new CpuSample(timestampMillis, usageNanos));

            if (prev == null) {
                return;
            }

            final long deltaUsageNanos = usageNanos - prev.usageNanos;
            final long deltaTimeNanos = (timestampMillis - prev.timestampMillis) * 1_000_000L;
            final double cpuLimit = cgroup.getEffectiveCpus();
            final double effectiveCpus = cpuLimit > 0 ? cpuLimit : Runtime.getRuntime().availableProcessors();

            if (deltaUsageNanos >= 0 && deltaTimeNanos > 0 && effectiveCpus > 0) {
                final double cpuPercent = (double) deltaUsageNanos / (deltaTimeNanos * effectiveCpus) * 100.0;
                final int roundedCpuPercent = (int) Math.min(100L, Math.max(0L, Math.round(cpuPercent)));
                metrics.put(NodeStatMetrics.CPU_PERCENT.getFieldName(), roundedCpuPercent);
            }
        } catch (RuntimeException e) {
            log.debug("Cgroup CPU metrics not available for node {}: {}", node, e.getMessage());
        }
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
