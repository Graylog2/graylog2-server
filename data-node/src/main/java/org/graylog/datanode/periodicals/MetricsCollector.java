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

import jakarta.inject.Inject;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessState;
import org.graylog.shaded.opensearch2.org.joda.time.DateTime;
import org.graylog.shaded.opensearch2.org.joda.time.DateTimeZone;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.index.IndexResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.core.action.ActionListener;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsCollector.class);
    private final OpensearchProcess process;
    private final Configuration configuration;
    private final NodeId nodeId;

    @Inject
    public MetricsCollector(OpensearchProcess process, Configuration configuration, NodeId nodeId) {
        this.process = process;
        this.configuration = configuration;
        this.nodeId = nodeId;
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
                final IndexRequest indexRequest = new IndexRequest(configuration.getMetricsStream());
                Map<String, Object> metrics = new HashMap<String, Object>();
                metrics.put(configuration.getMetricsTimestamp(), new DateTime(DateTimeZone.UTC));
                metrics.put("node", configuration.getHostname());
                metrics.put("jvm_heap", Runtime.getRuntime().totalMemory());
                indexRequest.source(metrics);
                client.indexAsync(indexRequest, RequestOptions.DEFAULT, new ActionListener<IndexResponse>() {
                    @Override
                    public void onResponse(IndexResponse indexResponse) {
                    }

                    @Override
                    public void onFailure(Exception e) {
                        LOG.error("Error indexing metrics", e);
                    }
                });
            });
        }
    }
}
