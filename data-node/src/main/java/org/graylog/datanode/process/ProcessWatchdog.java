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
package org.graylog.datanode.process;

import org.graylog.datanode.management.ManagedNodes;
import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ProcessWatchdog {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessWatchdog.class);

    @Autowired
    private ManagedNodes managedOpenSearch;

    @Scheduled(fixedRate = 1000)
    public void monitorProcess() {
        managedOpenSearch.getProcesses()
                .forEach(this::updateStatus);
    }

    @Scheduled(fixedRate = 10_000)
    public void heartbeat() {
        managedOpenSearch.getProcesses()
                .stream().filter(p -> p.getStatus() == ProcessStatus.STARTED)
                .forEach(process -> {
                    try {
                        final ClusterHealthResponse health = process.getRestClient().cluster().health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
                        if (health.getStatus() == ClusterHealthStatus.GREEN) {
                            process.setStatus(ProcessStatus.RUNNING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void updateStatus(OpensearchProcess process) {
        if (!process.getProcess().isAlive()) {
            process.setStatus(ProcessStatus.TERMINATED);
        }
    }
}
