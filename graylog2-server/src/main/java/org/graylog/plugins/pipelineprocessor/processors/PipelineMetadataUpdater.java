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
import org.graylog.plugins.pipelineprocessor.db.RoutingRuleDao;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.resources.system.inputs.InputDeletedEvent;
import org.graylog2.rest.resources.system.inputs.InputRenamedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Update the metadata when a pipeline, connection, rule or input is changed:
 * - for every deleted input: delete the corresponding input metadata record
 * - for every deleted pipeline: delete the corresponding pipeline rules metadata record and update any input
 * metadata that referenced that pipeline
 * - for every updated pipeline:
 * -- rebuild pipeline metadata from scratch and replace the existing record
 * -- for every input referenced by the pipeline: update the input metadata to include a mention of that pipeline
 * - for every deleted or updated rule:
 * -- delete the input mentions for that rule
 * -- for every pipeline affected by the deleted or updated rule: rebuild the pipeline metadata
 * - for every updated rule: rebuild the input metadata. The rule may no longer reference the same input - or any
 * input - so it's easiest to rebuild from scratch.
 */
@Singleton
public class PipelineMetadataUpdater {

    private final MongoDbPipelineMetadataService pipelineMetadataService;
    private final MongoDbInputsMetadataService inputsMetadataService;
    private final PipelineAnalyzer pipelineAnalyzer;
    private final PipelineService pipelineService;
    protected final EventBus eventBus;

    @Inject
    public PipelineMetadataUpdater(MongoDbPipelineMetadataService pipelineMetadataService,
                                   MongoDbInputsMetadataService inputsMetadataService,
                                   PipelineAnalyzer pipelineAnalyzer,
                                   PipelineService pipelineService,
                                   EventBus eventBus) {
        this.pipelineMetadataService = pipelineMetadataService;
        this.inputsMetadataService = inputsMetadataService;
        this.pipelineAnalyzer = pipelineAnalyzer;
        this.pipelineService = pipelineService;
        this.eventBus = eventBus;

        eventBus.register(this);
    }

    public void handlePipelineChanges(PipelinesChangedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        deletePipelineEntries(event);
        deleteInputMentionsForPipelines(event);
        Set<PipelineDao> pipelineDaos = affectedPipelines(event);
        handleUpdates(pipelineDaos, state, resolver, metricRegistry);
    }

    public void handleConnectionChanges(PipelineConnectionsChangedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        Set<PipelineDao> pipelineDaos = affectedPipelines(event);
        handleUpdates(pipelineDaos, state, resolver, metricRegistry);
    }

    public void handleRuleChanges(RulesChangedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        deleteInputMentionsForRules(event);
        handleUpdates(affectedPipelines(event), state, resolver, metricRegistry);
    }

    /**
     * When an input is deleted:
     * - remove the metadata record for that input
     * - pipelines that referenced that input need to be re-analyzed to potentially reset the has_input_references flag
     */
    public void handleInputDeleted(InputDeletedEvent event, PipelineInterpreter.State state, PipelineResolver resolver, PipelineMetricRegistry metricRegistry) {
        handleUpdates(affectedPipelines(event), state, resolver, metricRegistry);
        inputsMetadataService.deleteInput(event.inputId());
    }

    private void deleteInputMentionsForPipelines(PipelinesChangedEvent event) {
        Stream.concat(event.deletedPipelineIds().stream(), event.updatedPipelineIds().stream())
                .forEach(inputsMetadataService::deleteInputMentionsByPipelineId);
    }

    private void deleteInputMentionsForRules(RulesChangedEvent event) {
        Stream.concat(event.deletedRules().stream(), event.updatedRules().stream())
                .forEach(ref -> inputsMetadataService.deleteInputMentionsByRuleId(ref.id()));
    }

    protected void handleUpdates(Set<PipelineDao> pipelineDaos,
                               PipelineInterpreter.State state,
                               PipelineResolver resolver,
                               PipelineMetricRegistry metricRegistry) {
        ImmutableMap<String, Pipeline> pipelines = affectedPipelinesAsMap(pipelineDaos, state);
        ImmutableMap<String, Pipeline> functions = resolver.resolveFunctions(pipelines.values(), metricRegistry);
        List<PipelineRulesMetadataDao> ruleRecords = new ArrayList<>();
        List<RoutingRuleDao> routingRuleRecords = new ArrayList<>();
        Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions =
                pipelineAnalyzer.analyzePipelines(pipelines, functions, ruleRecords, routingRuleRecords);

        inputsMetadataService.save(inputMentions, true);
        pipelineMetadataService.save(ruleRecords, routingRuleRecords, true);
    }

    private Set<PipelineDao> loadPipelineDaos(Set<String> ids) {
        return ids.stream()
                .map(id -> {
                    try {
                        return pipelineService.load(id);
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    protected Set<PipelineDao> affectedPipelines(RulesChangedEvent event) {
        Set<String> ruleIds = event.updatedRules().stream()
                .map(RulesChangedEvent.Reference::id)
                .collect(Collectors.toSet());
        Set<String> pipelineIds = pipelineMetadataService.getPipelinesByRules(ruleIds);
        return loadPipelineDaos(pipelineIds);
    }

    private Set<PipelineDao> affectedPipelines(PipelinesChangedEvent event) {
        return loadPipelineDaos(event.updatedPipelineIds());
    }

    private Set<PipelineDao> affectedPipelines(PipelineConnectionsChangedEvent event) {
        return loadPipelineDaos(event.pipelineIds());
    }

    private Set<PipelineDao> affectedPipelines(InputDeletedEvent event) {
        PipelineInputsMetadataDao inputDao;
        try {
            inputDao = inputsMetadataService.getByInputId(event.inputId());
        } catch (NotFoundException e) {
            return Set.of();
        }
        Set<String> affectedPipelineIds = inputDao.mentionedIn().stream()
                .map(PipelineInputsMetadataDao.MentionedInEntry::pipelineId)
                .collect(Collectors.toSet());
        return loadPipelineDaos(affectedPipelineIds);
    }

    protected ImmutableMap<String, Pipeline> affectedPipelinesAsMap(Set<PipelineDao> pipelineDaos, PipelineInterpreter.State state) {
        ImmutableMap.Builder<String, Pipeline> builder = ImmutableMap.builder();
        for (PipelineDao pipelineDao : pipelineDaos) {
            Pipeline pipeline = state.getCurrentPipelines().get(pipelineDao.id());
            if (pipeline != null) {
                builder.put(Objects.requireNonNull(pipelineDao.id()), pipeline);
            }
        }
        return builder.build();
    }

    /**
     * When a pipeline is deleted, remove the metadata record for that pipeline
     */
    private void deletePipelineEntries(PipelinesChangedEvent event) {
        pipelineMetadataService.delete(event.deletedPipelineIds());
    }

    /**
     * When an input is renamed, rules that previously referenced the input by name are no longer applicable; and
     * rules that reference the new name now apply.
     * Unfortunately, we don't have an exact mapping of rules by referenced input name. So we find all the rules
     * that reference any inputs by name and fire an update event for those rules.
     * Note: we also don't have an exact mapping of functions to rules, so we just trigger for all the rules included
     * in pipelines that reference inputs in any way.
     */
    @Subscribe
    public void handleInputRename(InputRenamedEvent event) {
        Set<RulesChangedEvent.Reference> updated = pipelineMetadataService.getReferencingPipelines().stream()
                .flatMap(dao -> dao.rules().stream())
                .filter(Objects::nonNull)
                .map(ruleId -> new RulesChangedEvent.Reference(ruleId, ruleId))
                .collect(Collectors.toSet());
        eventBus.post(new RulesChangedEvent(updated, Set.of()));
    }
}
