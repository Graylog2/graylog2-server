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
import org.graylog.datanode.periodicals.MetricsCollector;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.storage.opensearch2.DataStreamAdapterOS2;
import org.graylog.storage.opensearch2.ism.IsmApi;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.datastream.DataStreamService;
import org.graylog2.indexer.datastream.DataStreamServiceImpl;
import org.graylog2.indexer.datastream.policy.IsmPolicy;
import org.graylog2.indexer.datastream.policy.Policy;
import org.graylog2.indexer.datastream.policy.actions.Action;
import org.graylog2.indexer.datastream.policy.actions.DeleteAction;
import org.graylog2.indexer.datastream.policy.actions.RolloverAction;
import org.graylog2.indexer.datastream.policy.actions.RollupAction;
import org.graylog2.indexer.datastream.policy.actions.TimesUnit;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigureMetricsIndexSettings implements StateMachineTracer {

    private final Logger log = LoggerFactory.getLogger(ConfigureMetricsIndexSettings.class);

    private final OpensearchProcess process;
    private final Configuration configuration;
    private final IndexFieldTypesService indexFieldTypesService;
    private final ObjectMapper objectMapper;
    private DataStreamService dataStreamService;
    private final NodeService<DataNodeDto> nodeService;

    public ConfigureMetricsIndexSettings(OpensearchProcess process, Configuration configuration, IndexFieldTypesService indexFieldTypesService, ObjectMapper objectMapper, NodeService<DataNodeDto> nodeService) {
        this.process = process;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        this.indexFieldTypesService = indexFieldTypesService;
        this.nodeService = nodeService;
    }

    @Override
    public void trigger(ProcessEvent trigger) {
    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        if (destination == ProcessState.AVAILABLE && source == ProcessState.STARTING) {
            process.openSearchClient().ifPresent(client -> {
                final IsmApi ismApi = new IsmApi(client, objectMapper);
                int replicas = nodeService.allActive().size() == 1 ? 0 : 1;
                dataStreamService = new DataStreamServiceImpl(
                        new DataStreamAdapterOS2(client, objectMapper, ismApi),
                        indexFieldTypesService,
                        replicas
                );
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

        mappings.putAll(MetricsCollector.getDatanodeMetrics());

        mappings.putAll(
                Arrays.stream(NodeStatMetrics.values()).collect(Collectors.toMap(
                        NodeStatMetrics::getFieldName, metric -> ImmutableMap.of("type", metric.getMappingType())
                ))
        );
        mappings.putAll(
                Arrays.stream(ClusterStatMetrics.values()).collect(Collectors.toMap(
                        ClusterStatMetrics::getFieldName, metric -> ImmutableMap.of("type", metric.getMappingType())
                ))
        );
        mappings.putAll(
                Arrays.stream(ClusterStatMetrics.values()).filter(ClusterStatMetrics::isRateMetric).collect(Collectors.toMap(
                        ClusterStatMetrics::getRateFieldName, metric -> ImmutableMap.of("type", metric.getMappingType())
                ))
        );
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
                stateOpen.name(), ImmutableList.of(stateOpen, stateRollup, stateDelete),
                null);

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
        List<RollupAction.IsmRollup.Metric> rollupFields = Arrays.stream(NodeStatMetrics.values())
                .map(metric -> new RollupAction.IsmRollup.Metric(metric.getFieldName(), ImmutableList.of(metric.getAggregationMetric())))
                .collect(Collectors.toList());
        rollupFields.addAll(Arrays.stream(ClusterStatMetrics.values())
                .map(metric -> new RollupAction.IsmRollup.Metric(metric.getFieldName(), ImmutableList.of(metric.getAggregationMetric())))
                .toList());

        final RollupAction.IsmRollup ismRollup = new RollupAction.IsmRollup(
                configuration.getMetricsDailyIndex(),
                "Rollup Data Stream Index",
                1000,
                ImmutableList.of(
                        new RollupAction.IsmRollup.DateHistogram(
                                configuration.getMetricsTimestamp(),
                                "60m", "UTC")
                ),
                rollupFields
        );
        RollupAction rollupAction = new RollupAction(ismRollup);
        final List<Action> actions = ImmutableList.of(new Action(rollupAction));
        final List<Policy.Transition> transitions = ImmutableList.of(new Policy.Transition(nextState,
                new Policy.Condition(TimesUnit.DAYS.format(configuration.getMetricsRetention().toDays()))));
        return new Policy.State("rollup", actions, transitions);
    }

    private Policy.State ismOpenState(String nextState) {
        final List<Action> actions = ImmutableList.of(new Action(new RolloverAction("1d", null)));
        final List<Policy.Transition> transitions = ImmutableList.of(new Policy.Transition(nextState, null));
        return new Policy.State("open", actions, transitions);
    }

}
