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
package org.graylog.datanode.periodicals;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.metrics.ClusterStatMetricsCollector;
import org.graylog.datanode.metrics.NodeMetricsCollector;
import org.graylog.datanode.metrics.NodeStatMetrics;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.graylog.storage.opensearch3.OSSerializationUtils;
import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsCollector extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);
    private final OpensearchProcess process;
    private final Configuration configuration;
    private NodeMetricsCollector nodeStatMetricsCollector;
    private ClusterStatMetricsCollector clusterStatMetricsCollector;
    private final ObjectMapper objectMapper;
    private final MetricRegistry metricRegistry;

    private final static Map<String, Object> opensearchMetrics = new ConcurrentHashMap<>();

    @Inject
    public MetricsCollector(OpensearchProcess process, Configuration configuration, ObjectMapper objectMapper, MetricRegistry metricRegistry) {
        this.process = process;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.metricRegistry = metricRegistry;
        registerNodeStatMetrics();
    }

    private void registerNodeStatMetrics() {
        Arrays.stream(NodeStatMetrics.values())
                .map(NodeStatMetrics::getMetricRegistryName)
                .forEach(metric -> {
                    metricRegistry.registerGauge(metric, new Gauge<Object>() {
                        @Override
                        public Object getValue() {
                            return getNodeMetric(metric);
                        }
                    });
                });
    }

    private Object getNodeMetric(String metricName) {
        return opensearchMetrics.getOrDefault(metricName, 0L);
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 60;
    }

    @Nonnull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        if (process.isInState(OpensearchState.AVAILABLE)) {
            process.openSearchClient().ifPresent(client -> {
                this.nodeStatMetricsCollector = new NodeMetricsCollector(client, objectMapper);
                this.clusterStatMetricsCollector = new ClusterStatMetricsCollector(client, objectMapper);
                Map<String, Object> metrics = new HashMap<String, Object>();
                metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                String node = configuration.getDatanodeNodeName();
                metrics.put("node", node);
                addJvmMetrics(metrics);
                Map<String, Object> nodeMetrics = nodeStatMetricsCollector.getNodeMetrics(node);
                metrics.putAll(nodeMetrics);
                final Map<String, Object> finalMetrics = metrics;
                indexDocument(client, IndexRequest.of(i -> i
                        .index(configuration.getMetricsStream())
                        .document(finalMetrics)
                ));

                if (process.isManagerNode()) {
                    metrics = new HashMap<>(clusterStatMetricsCollector.getClusterMetrics(getPreviousMetricsForCluster(client)));
                    metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                    final Map<String, Object> clusterMetrics = metrics;
                    indexDocument(client, IndexRequest.of(i -> i
                            .index(configuration.getMetricsStream())
                            .document(clusterMetrics)
                    ));
                }

                nodeMetrics.forEach((key, value) -> {
                    opensearchMetrics.put(NodeStatMetrics.getMetricRegistryName(key), value);
                });
            });
        }
    }

    private void addJvmMetrics(Map<String, Object> metrics) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        metrics.put("dn_heap_usage", calcUsage(memoryMXBean.getHeapMemoryUsage()));
        metrics.put("dn_non_heap_usage", calcUsage(memoryMXBean.getNonHeapMemoryUsage()));

        Runtime runtime = Runtime.getRuntime();
        metrics.put("dn_processors", runtime.availableProcessors());

        metrics.put("dn_thread_count", Thread.activeCount());

        long gcTime = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .sum();
        metrics.put("dn_gc_time", gcTime);
    }


    public static Map<String, Map<String, String>> getDatanodeMetrics() {
        return Map.of(
                "dn_heap_usage", ImmutableMap.of("type", "float"),
                "dn_non_heap_usage", ImmutableMap.of("type", "float"),
                "dn_processors", ImmutableMap.of("type", "integer"),
                "dn_thread_count", ImmutableMap.of("type", "integer"),
                "dn_gc_time", ImmutableMap.of("type", "long")
        );
    }

    private float calcUsage(MemoryUsage memoryUsage) {
        return 100 * (float) memoryUsage.getUsed() / memoryUsage.getCommitted();
    }

    private Map<String, Object> getPreviousMetricsForCluster(OfficialOpensearchClient client) {
        SearchRequest searchRequest = SearchRequest.of(r -> r
                .index(configuration.getMetricsStream())
                .query(Query.of(q -> q.bool(b -> b.must(Query.of(q2 -> q2.exists(e -> e.field("node")))))))
                .size(1)
                .sort(sort -> sort.field(f -> f
                                .field(configuration.getMetricsTimestamp())
                                .order(SortOrder.Desc)
                        )
                )
        );
        SearchResponse<JsonData> searchResponse = null;
        try {
            searchResponse = client.syncWithoutErrorMapping().search(searchRequest, JsonData.class);
        } catch (IOException e) {
            LOG.error("Could not retrieve previous metrics", e);
        }

        if (Objects.nonNull(searchResponse) && searchResponse.hits().total().value() > 0) {
            // Retrieve the first hit (latest document) from the search response
            return OSSerializationUtils.toMap(searchResponse.hits().hits().getFirst());
        } else {
            LOG.info("No previous metrics for cluster");
        }
        return Map.of();
    }

    private static void indexDocument(OfficialOpensearchClient client, IndexRequest<Object> indexRequest) {
        try {
            CompletableFuture<IndexResponse> response = client.asyncWithoutErrorMapping().index(indexRequest);
            if (response.isCompletedExceptionally()) {
                LOG.error("Error indexing metrics");
            }
        } catch (IOException e) {
            LOG.error("Error indexing metrics", e);
        }
    }
}
