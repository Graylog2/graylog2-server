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
package org.graylog.datanode.management;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.shaded.opensearch2.org.opensearch.OpenSearchStatusException;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterGetSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.graylog.shaded.opensearch2.org.opensearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.ClusterClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This tracer triggers functionality for node removal.
 * It triggers removal on PROCESS_REMOVE and will reset the allocation exclude setting in OS on successful start of OS,
 * which cannot be done after the removal due to the unavailability of the OS REST api.
 */
public class OpensearchRemovalTracer implements StateMachineTracer {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchRemovalTracer.class);
    static final String CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING = "cluster.routing.allocation.exclude._name";

    private final OpensearchProcess process;
    private final String nodeName;
    boolean allocationExcludeChecked = false;
    ScheduledExecutorService executorService;

    public OpensearchRemovalTracer(OpensearchProcess process, String nodeName) {
        this.process = process;
        this.nodeName = nodeName;
    }


    @Override
    public void trigger(ProcessEvent trigger) {
        LOG.debug("Removal tracer trigger: {}", trigger);
    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        if (destination == ProcessState.AVAILABLE && !allocationExcludeChecked) {
            checkAllocationEnabledStatus();
        } else if (trigger == ProcessEvent.PROCESS_REMOVE) {
            triggerRemoval();
        }
    }

    private void checkAllocationEnabledStatus() {
        if (process.restClient().isPresent()) {
            ClusterClient clusterClient = process.restClient().get().cluster();
            try {
                final ClusterGetSettingsResponse settings =
                        clusterClient.getSettings(new ClusterGetSettingsRequest(), RequestOptions.DEFAULT);
                final String setting = settings.getSetting(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING);
                if (nodeName.equals(setting)) {
                    ClusterUpdateSettingsRequest updateSettings = new ClusterUpdateSettingsRequest();
                    updateSettings.transientSettings(Settings.builder()
                            .putNull(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING)
                            .build());
                    clusterClient.putSettings(updateSettings, RequestOptions.DEFAULT);
                }
                allocationExcludeChecked = true;
            } catch (IOException e) {
                LOG.error("Error getting cluster settings from OpenSearch");
            }
        }
    }

    private void triggerRemoval() {
        process.restClient().ifPresent(client -> {
            final ClusterClient clusterClient = client.cluster();
            ClusterUpdateSettingsRequest settings = new ClusterUpdateSettingsRequest();
            settings.transientSettings(Settings.builder()
                    .put(CLUSTER_ROUTING_ALLOCATION_EXCLUDE_SETTING, nodeName)
                    .build());
            try {
                final ClusterUpdateSettingsResponse response =
                        clusterClient.putSettings(settings, RequestOptions.DEFAULT);
                if (response.isAcknowledged()) {
                    allocationExcludeChecked = false; // reset to rejoin cluster in case of failure
                    executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("datanode-removal").build());
                    executorService.scheduleAtFixedRate(this::checkRemovalStatus, 10, 10, TimeUnit.SECONDS);
                } else {
                    process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
                }
            } catch (IOException e) {
                LOG.error("Failed to exclude node from cluster allocation", e);
                process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
            }
        });
    }

    void checkRemovalStatus() {
        final Optional<RestHighLevelClient> restClient = process.restClient();
        if (restClient.isPresent()) {
            try {
                final ClusterClient clusterClient = restClient.get().cluster();
                final ClusterHealthResponse health = clusterClient
                        .health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
                if (health.getRelocatingShards() == 0) {
                    process.stop();
                    executorService.shutdown();
                }
            } catch (IOException | OpenSearchStatusException e) {
                process.onEvent(ProcessEvent.HEALTH_CHECK_FAILED);
            }
        }
    }

}
