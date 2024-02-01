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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.storage.opensearch2.DataStreamAdapterOS2;
import org.graylog.storage.opensearch2.ism.IsmApi;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicy;
import org.graylog.storage.opensearch2.ism.policy.Policy;
import org.graylog.storage.opensearch2.ism.policy.actions.Action;
import org.graylog.storage.opensearch2.ism.policy.actions.DeleteAction;
import org.graylog.storage.opensearch2.ism.policy.actions.RolloverAction;
import org.graylog.storage.opensearch2.ism.policy.actions.RollupAction;
import org.graylog2.indexer.datastream.DataStreamService;
import org.graylog2.indexer.datastream.DataStreamServiceImpl;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigureMetricsIndexSettings implements StateMachineTracer {

    private final Logger log = LoggerFactory.getLogger(ConfigureMetricsIndexSettings.class);

    private final OpensearchProcess process;
    private final Configuration configuration;
    private final IndexFieldTypesService indexFieldTypesService;
    private final ObjectMapper objectMapper;
    private DataStreamService dataStreamService;

    public ConfigureMetricsIndexSettings(OpensearchProcess process, Configuration configuration, IndexFieldTypesService indexFieldTypesService, ObjectMapper objectMapper) {
        this.process = process;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.indexFieldTypesService = indexFieldTypesService;
    }

    @Override
    public void trigger(ProcessEvent trigger) {
    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        if (destination == ProcessState.AVAILABLE && source == ProcessState.STARTING) {
            process.openSearchClient().ifPresent(client -> {
                if (dataStreamService == null) {
                    final IsmApi ismApi = new IsmApi(client, objectMapper);
                    dataStreamService = new DataStreamServiceImpl(
                            new DataStreamAdapterOS2(client, objectMapper, ismApi),
                            indexFieldTypesService
                    );
                }
                dataStreamService.createDataStream(configuration.getMetricsStream(),
                        configuration.getMetricsTimestamp(),
                        createMappings(),
                        createPolicy(configuration));
            });
        }
    }

    private Map<String, Map<String, String>> createMappings() {
        Map<String, Map<String, String>> mappings = new HashMap<>();
        mappings.put("node", ImmutableMap.of("type", "keyword"));
        return mappings;
    }

    private IsmPolicy createPolicy(Configuration configuration) {
        // states defined from last to first to use name in previous step
        Policy.State stateDelete = ismDeleteState();
        Policy.State stateRollup = ismRollupState(stateDelete.name(), configuration);
        Policy.State stateOpen = ismOpenState(stateRollup.name());

        Policy policy = new Policy(null,
                "Manages rollover, rollup and deletion of data note metrics indices",
                null,
                stateOpen.name(), ImmutableList.of(stateOpen, stateRollup, stateDelete));

        try {
            log.debug("Creating ISM configuration for metrics data stream {}",
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policy));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return new IsmPolicy(configuration.getMetricsPolicy(), policy);
    }

    private Policy.State ismDeleteState() {
        final List<Action> actions = ImmutableList.of(new Action(new DeleteAction()));
        final List<Policy.Transition> transitions = ImmutableList.of();
        return new Policy.State("delete", actions, transitions);
    }

    private Policy.State ismRollupState(String nextState, Configuration configuration) {

        final RollupAction.IsmRollup ismRollup = new RollupAction.IsmRollup(
                configuration.getMetricsDailyIndex(),
                "Rollup Data Stream Index",
                1000,
                ImmutableList.of(
                        new RollupAction.IsmRollup.DateHistogram(
                                configuration.getMetricsTimestamp(),
                                "60m", "UTC")
                ),
                ImmutableList.of( //TODO dynamically rollup all configured metrics
                        new RollupAction.IsmRollup.Metric(
                                "jvm_heap",
                                ImmutableList.of(new RollupAction.IsmRollup.AvgMetric())
                        )
                )
        );
        RollupAction rollupAction = new RollupAction(ismRollup);
        final List<Action> actions = ImmutableList.of(new Action(rollupAction));
        final List<Policy.Transition> transitions = ImmutableList.of(new Policy.Transition(nextState, new Policy.Condition(configuration.getMetricsRetention())));
        return new Policy.State("rollup", actions, transitions);
    }

    private Policy.State ismOpenState(String nextState) {
        final List<Action> actions = ImmutableList.of(new Action(new RolloverAction("1d")));
        final List<Policy.Transition> transitions = ImmutableList.of(new Policy.Transition(nextState, null));
        return new Policy.State("open", actions, transitions);
    }

}
