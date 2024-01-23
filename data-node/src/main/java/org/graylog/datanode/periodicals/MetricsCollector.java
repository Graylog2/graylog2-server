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
import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.metrics.ClusterStatMetricsCollector;
import org.graylog.datanode.metrics.NodeStatMetricsCollector;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.joda.time.DateTime;
import org.graylog.shaded.opensearch2.org.joda.time.DateTimeZone;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.core.action.ActionListener;
import org.graylog2.plugin.periodical.Periodical;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);
    private final OpensearchProcess process;
    private final Configuration configuration;
    private NodeStatMetricsCollector nodeStatMetricsCollector;
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
                this.nodeStatMetricsCollector = new NodeStatMetricsCollector(client, objectMapper);
                this.clusterStatMetricsCollector = new ClusterStatMetricsCollector(client, objectMapper);
                this.isLeader = process.isLeaderNode();
                final IndexRequest indexRequest = new IndexRequest(configuration.getMetricsStream());
                Map<String, Object> metrics = new HashMap<String, Object>();
                metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                String node = configuration.getDatanodeNodeName();
                metrics.put("node", node);
                metrics.put("dn_jvm_heap", Runtime.getRuntime().totalMemory());
                metrics.putAll(nodeStatMetricsCollector.getNodeMetrics(node));
                indexRequest.source(metrics);
                indexDocument(client, indexRequest);

                if (isLeader) {
                    metrics = new HashMap<>(clusterStatMetricsCollector.getClusterMetrics());
                    metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                    indexRequest.source(metrics);
                    indexDocument(client, indexRequest);
                }

            });
        }
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
