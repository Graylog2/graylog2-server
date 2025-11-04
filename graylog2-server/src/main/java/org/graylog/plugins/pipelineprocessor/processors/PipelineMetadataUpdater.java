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
package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PipelineMetadataUpdater {

    private final MongoDbPipelineMetadataService metadataService;
    private final PipelineAnalyzer pipelineAnalyzer;
    private final PipelineService pipelineService;
    private final RuleService ruleService;

    @Inject
    public PipelineMetadataUpdater(MongoDbPipelineMetadataService metadataService,
                                   PipelineAnalyzer pipelineAnalyzer,
                                   PipelineService pipelineService,
                                   RuleService ruleService,
                                   EventBus eventBus) {
        this.metadataService = metadataService;
        this.pipelineAnalyzer = pipelineAnalyzer;
        this.pipelineService = pipelineService;
        this.ruleService = ruleService;

        eventBus.register(this);
    }

    public void handlePipelineChanges(PipelinesChangedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        Set<PipelineDao> pipelineDaos = affectedPipelines(event);
        handleUpdates(pipelineDaos, state, resolver, metricRegistry);
        handlePipelineDeletions(event);
    }

    public void handleRuleChanges(RulesChangedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        Set<RuleDao> ruleDaos = affectedRules(event);
        Set<PipelineDao> pipelineDaos = affectedPipelines(ruleDaos);
        handleUpdates(pipelineDaos, state, resolver, metricRegistry);
    }

    private void handleUpdates(Set<PipelineDao> pipelineDaos,
                               PipelineInterpreter.State state,
                               PipelineResolver resolver,
                               PipelineMetricRegistry metricRegistry) {
        ImmutableMap<String, Pipeline> pipelines = affectedPipelinesAsMap(pipelineDaos, state);
        ImmutableMap<String, Pipeline> functions = resolver.resolveFunctions(pipelines.values(), metricRegistry);
        List<PipelineRulesMetadataDao> ruleRecords = new ArrayList<>();
        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions = pipelineAnalyzer.analyzePipelines(pipelines, functions, ruleRecords);

        metadataService.saveRulesMetadata(ruleRecords, true);
        metadataService.saveInputsMetadata(inputMentions, true);
    }

    private void handlePipelineDeletions(PipelinesChangedEvent event) {
        metadataService.deleteRulesMetadataByPipelineId(event.deletedPipelineIds());
    }

    private Set<RuleDao> affectedRules(RulesChangedEvent event) {
        Set<String> ruleIds = event.updatedRules().stream()
                .map(RulesChangedEvent.Reference::id)
                .collect(Collectors.toSet());

        return ruleIds.stream()
                .map(id -> {
                    try {
                        return ruleService.load(id);
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }

    private Set<PipelineDao> affectedPipelines(Set<RuleDao> rules) {
        return rules.stream()
                .map(RuleDao::title)
                .flatMap(title -> pipelineService.loadBySourcePattern(title).stream())
                .collect(Collectors.toSet());
    }

    private Set<PipelineDao> affectedPipelines(PipelinesChangedEvent event) {
        return event.updatedPipelineIds().stream()
                .map(id -> {
                    try {
                        return pipelineService.load(id);
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());
    }

    private ImmutableMap<String, Pipeline> affectedPipelinesAsMap(Set<PipelineDao> pipelineDaos, PipelineInterpreter.State state) {
        ImmutableMap.Builder<String, Pipeline> builder = ImmutableMap.builder();
        for (PipelineDao pipelineDao : pipelineDaos) {
            Pipeline pipeline = state.getCurrentPipelines().get(pipelineDao.id());
            if (pipeline != null) {
                builder.put(pipelineDao.id(), pipeline);
            }
        }
        return builder.build();
    }

    @Subscribe
    public void handleInputDeleted(InputDeletedEvent event) {
        metadataService.deleteByInputId(event.inputId());
    }
}
