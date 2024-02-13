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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.metrics.ClusterStatMetricsCollector;
import org.graylog.datanode.metrics.NodeMetricsCollector;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.joda.time.DateTime;
import org.graylog.shaded.opensearch2.org.joda.time.DateTimeZone;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.core.action.ActionListener;
import org.graylog.shaded.opensearch2.org.opensearch.index.query.QueryBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.builder.SearchSourceBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.sort.SortOrder;
import org.graylog2.plugin.periodical.Periodical;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MetricsCollector extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);
    private final OpensearchProcess process;
    private final Configuration configuration;
    private NodeMetricsCollector nodeStatMetricsCollector;
    private ClusterStatMetricsCollector clusterStatMetricsCollector;
    private final ObjectMapper objectMapper;
    private boolean isLeader;

    @Inject
    public MetricsCollector(OpensearchProcess process, Configuration configuration, ObjectMapper objectMapper) {
        this.process = process;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
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

    @NotNull
    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        if (process.isInState(ProcessState.AVAILABLE)) {
            process.restClient().ifPresent(client -> {
                this.nodeStatMetricsCollector = new NodeMetricsCollector(client, objectMapper);
                this.clusterStatMetricsCollector = new ClusterStatMetricsCollector(client, objectMapper);
                this.isLeader = process.isLeaderNode();

                final IndexRequest indexRequest = new IndexRequest(configuration.getMetricsStream());
                Map<String, Object> metrics = new HashMap<String, Object>();
                metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                String node = configuration.getDatanodeNodeName();
                metrics.put("node", node);
                addJvmMetrics(metrics);
                metrics.putAll(nodeStatMetricsCollector.getNodeMetrics(node));
                indexRequest.source(metrics);
                indexDocument(client, indexRequest);

                if (isLeader) {
                    metrics = new HashMap<>(clusterStatMetricsCollector.getClusterMetrics(getPreviousMetricsForCluster(client)));
                    metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                    indexRequest.source(metrics);
                    indexDocument(client, indexRequest);
                }

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

    private Map<String, Object> getPreviousMetricsForCluster(RestHighLevelClient client) {
        SearchRequest searchRequest = new SearchRequest(configuration.getMetricsStream());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("node")));  // You can adjust the query based on your requirements
        searchSourceBuilder.size(1);  // Retrieve only one document
        searchSourceBuilder.sort(SortBuilders.fieldSort(configuration.getMetricsTimestamp()).order(SortOrder.DESC));  // Sort by timestamp in descending order
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            LOG.error("Could not retrieve previous metrics", e);
        }

        if (Objects.nonNull(searchResponse) && searchResponse.getHits().getTotalHits().value > 0) {
            // Retrieve the first hit (latest document) from the search response
            return searchResponse.getHits().getAt(0).getSourceAsMap();
        } else {
            LOG.info("No previous metrics for cluster");
        }
        return Map.of();
    }

    private static void indexDocument(RestHighLevelClient client, IndexRequest indexRequest) {
        client.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
            }

            @Override
            public void onFailure(Exception e) {
                LOG.error("Error indexing metrics", e);
            }
        });
    }
}
