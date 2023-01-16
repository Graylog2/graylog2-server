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
    public void opensearchApiHeartbeat() {
        managedOpenSearch.getProcesses()
                .forEach(process -> {
                    try {
                        final ClusterHealthRequest req = new ClusterHealthRequest();
                        final ClusterHealthResponse health = process.getRestClient()
                                .cluster()
                                .health(req, RequestOptions.DEFAULT);
                        onClusterStatus(process, health);
                    } catch (IOException e) {
                        onRestError(process, e);
                    }
                });
    }

    private void onClusterStatus(OpensearchProcess process, ClusterHealthResponse health) {
        if (process.getStatus() == ProcessState.AVAILABLE && health.getStatus() == ClusterHealthStatus.GREEN) {
            process.onEvent(ProcessEvent.HEALTH_CHECK_GREEN);
        }
    }

    private void onRestError(OpensearchProcess process, IOException e) {
        LOG.warn("Opensearch REST api of process {} unavailable. Cause: {}", process.getProcessInfo().pid(), e.getMessage());
    }

    private void updateStatus(OpensearchProcess process) {
        if (!process.getProcess().isAlive()) {
            process.onEvent(ProcessEvent.PROCESS_TERMINATED);
        }
    }
}
