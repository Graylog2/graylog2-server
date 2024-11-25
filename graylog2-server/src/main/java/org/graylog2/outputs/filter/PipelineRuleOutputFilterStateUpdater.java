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
package org.graylog2.outputs.filter;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog2.outputs.filter.functions.RemoveFromStreamDestination;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.streams.filters.StreamDestinationFilterRuleDTO;
import org.graylog2.streams.filters.StreamDestinationFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * State updater for the {@link PipelineRuleOutputFilter}.
 */
@Singleton
public class PipelineRuleOutputFilterStateUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRuleOutputFilterStateUpdater.class);

    private final StreamDestinationFilterService filterService;
    private final PipelineRuleOutputFilterState.Factory stateFactory;
    private final Map<String, FilteredMessageOutput> filteredOutputs;
    private final RuleBuilderService ruleBuilderService;
    private final PipelineResolver.Factory resolverFactory;
    private final PipelineRuleParser pipelineRuleParser;
    private final PipelineMetricRegistry pipelineMetricRegistry;

    @Inject
    public PipelineRuleOutputFilterStateUpdater(StreamDestinationFilterService filterService,
                                                PipelineRuleOutputFilterState.Factory stateFactory,
                                                Map<String, FilteredMessageOutput> filteredOutputs,
                                                RuleBuilderService ruleBuilderService,
                                                PipelineResolver.Factory resolverFactory,
                                                PipelineRuleParser pipelineRuleParser,
                                                MetricRegistry metricRegistry) {
        this.filterService = filterService;
        this.stateFactory = stateFactory;
        this.filteredOutputs = filteredOutputs;
        this.ruleBuilderService = ruleBuilderService;
        this.resolverFactory = resolverFactory;
        this.pipelineRuleParser = pipelineRuleParser;

        this.pipelineMetricRegistry = PipelineMetricRegistry.create(
                metricRegistry,
                name(PipelineRuleOutputFilter.class, "pipelines"),
                name(PipelineRuleOutputFilter.class, "rules")
        );
    }

    public void init(AtomicReference<PipelineRuleOutputFilterState> activeState) {
        reload(activeState, ReloadTrigger.empty());
    }

    public void reloadForUpdate(AtomicReference<PipelineRuleOutputFilterState> activeState,
                                Set<String> updatedFilterRuleIds) {
        reload(activeState, ReloadTrigger.updatedIds(updatedFilterRuleIds));
    }

    public void reloadForDelete(AtomicReference<PipelineRuleOutputFilterState> activeState,
                                Set<String> deletedFilterRuleIds) {
        reload(activeState, ReloadTrigger.deletedIds(deletedFilterRuleIds));
    }

    private synchronized void reload(AtomicReference<PipelineRuleOutputFilterState> activeState,
                                     ReloadTrigger reloadTrigger) {
        LOG.debug("Reloading filter rules: {}", reloadTrigger);

        final var streamPipelines = new HashMap<String, Pipeline>();
        final var allRules = new ArrayList<RuleDao>();
        // We track active and previously active stream IDs, so we can clean up the metrics.
        final var activeStreams = new HashSet<String>();
        final var previouslyActiveStreams = getPreviouslyActiveStreams(activeState);

        // TODO: Only load rules where the stream still exists and is active!
        filterService.forEachEnabledFilterGroupedByStream(streamGroup -> {
            LOG.debug("Processing stream group: {}", streamGroup);

            final var streamId = streamGroup.streamId();
            final var ruleList = streamGroup.filters()
                    .stream()
                    .map(this::streamDestinationFilterToRuleDao)
                    .toList();

            final var filterPipeline = Pipeline.builder()
                    // We use the stream ID as pipeline ID, so we can easily select the pipelines we need to
                    // run for a message.
                    .id(streamId)
                    .name("Stream Destination Filter: " + streamId)
                    .stages(ImmutableSortedSet.of(
                            Stage.builder()
                                    .stage(0)
                                    .match(Stage.Match.EITHER)
                                    .ruleReferences(ruleList.stream().map(RuleDao::title).toList())
                                    .build()
                    ))
                    .build();

            streamPipelines.put(streamId, filterPipeline);
            allRules.addAll(ruleList);
            activeStreams.add(streamId);
        });

        LOG.debug("Stream pipelines: {}", streamPipelines);
        LOG.debug("Rule list: {}", allRules);
        LOG.debug("Active streams: {}", activeStreams);

        final var resolver = resolverFactory.create(
                PipelineResolverConfig.of(allRules::stream, java.util.stream.Stream::of),
                pipelineRuleParser
        );

        // This activates the new state! Only do cleanup after this statement.
        activeState.set(stateFactory.newState(
                resolver.resolveFunctions(streamPipelines.values(), pipelineMetricRegistry),
                ImmutableSet.copyOf(filteredOutputs.keySet()),
                ImmutableSet.copyOf(activeStreams)
        ));

        // Cleanup metrics for old pipeline and rule IDs to avoid stale entries in the metric registry.
        reloadTrigger.deletedIds().forEach(ruleId -> {
            LOG.debug("Removing rule metrics for: {}", ruleId);
            pipelineMetricRegistry.removeRuleMetrics(ruleId);
        });
        Sets.difference(previouslyActiveStreams, activeStreams).forEach(pipelineId -> {
            LOG.debug("Removing pipeline metrics for: {}", pipelineId);
            pipelineMetricRegistry.removePipelineMetrics(pipelineId);
        });
    }

    private RuleDao streamDestinationFilterToRuleDao(StreamDestinationFilterRuleDTO dto) {
        // The filter rules are exclude rules. For each matching rule we remove the filter rule's stream from the
        // destination.
        final var removalAction = RuleBuilderStep.builder()
                .id(UUID.randomUUID().toString())
                .title(UUID.randomUUID().toString())
                .function(RemoveFromStreamDestination.NAME)
                .parameters(Map.of(
                        RemoveFromStreamDestination.STREAM_ID_PARAM, dto.streamId(),
                        RemoveFromStreamDestination.DESTINATION_TYPE_PARAM, dto.destinationType()
                ))
                .build();

        // The removal action must be added to the rule, so it's executed when the rule condition matches.
        final var ruleWithRemovalAction = dto.rule().toBuilder().actions(List.of(removalAction)).build();

        // The uniqueness of the dto.title() is no guaranteed.
        final var title = f("[%s] %s", dto.id(), dto.title());
        return RuleDao.builder()
                .id(dto.id())
                .title(title)
                .source(ruleBuilderService.generateRuleSource(title, ruleWithRemovalAction, false))
                .build();
    }

    private Set<String> getPreviouslyActiveStreams(AtomicReference<PipelineRuleOutputFilterState> activeState) {
        return activeState.get() != null ? activeState.get().getActiveStreams() : Set.of();
    }

    private record ReloadTrigger(Set<String> updatedIds, Set<String> deletedIds) {
        public static ReloadTrigger empty() {
            return new ReloadTrigger(Set.of(), Set.of());
        }

        public static ReloadTrigger updatedIds(Set<String> updatedIds) {
            return new ReloadTrigger(updatedIds, Set.of());
        }

        public static ReloadTrigger deletedIds(Set<String> deletedIds) {
            return new ReloadTrigger(Set.of(), deletedIds);
        }
    }
}
