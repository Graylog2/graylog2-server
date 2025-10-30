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
package org.graylog.plugins.pipelineprocessor.processors.listeners;

import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.processors.PipelineAnalyzer;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class PipelineMetadataListener {

    private final MongoDbPipelineMetadataService metadataService;
    private final PipelineAnalyzer pipelineAnalyzer;

    @Inject
    public PipelineMetadataListener(MongoDbPipelineMetadataService metadataService,
                                    PipelineAnalyzer pipelineAnalyzer) {
        this.metadataService = metadataService;
        this.pipelineAnalyzer = pipelineAnalyzer;
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        Set<String> pipelines = new HashSet<>();
        event.deletedRules().forEach(rule -> {
            pipelines.addAll(metadataService.getPipelinesByRule(rule.id()));
        });
        event.updatedRules().forEach(rule -> {
            pipelines.addAll(metadataService.getPipelinesByRule(rule.id()));
        });
        pipelines.forEach(pipelineId -> pipelineAnalyzer.analyzePipelines());
    }

}
