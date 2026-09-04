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
import com.google.common.collect.ImmutableSetMultimap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;

@Singleton
public class PipelineInterpreterStateBuilder {

    private final PipelineResolver pipelineResolver;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final PipelineInterpreter.State.Factory stateFactory;

    @Inject
    public PipelineInterpreterStateBuilder(RuleService ruleService,
                                           PipelineService pipelineService,
                                           PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                           PipelineRuleParser pipelineRuleParser,
                                           PipelineResolver.Factory pipelineResolverFactory,
                                           RuleMetricsConfigService ruleMetricsConfigService,
                                           PipelineInterpreter.State.Factory stateFactory) {
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.stateFactory = stateFactory;
        this.pipelineResolver = pipelineResolverFactory.create(
                PipelineResolverConfig.of(
                        // TODO: Implement a #streamAll method in the services to get a real database cursor instead of
                        //       loading all entries into memory.
                        () -> ruleService.loadAll().stream(),
                        () -> pipelineService.loadAll().stream(),
                        () -> pipelineStreamConnectionsService.loadAll().stream()
                ),
                pipelineRuleParser
        );
    }

    public PipelineInterpreter.State buildState(PipelineMetricRegistry pipelineMetricRegistry) {
        final ImmutableMap<String, Pipeline> currentPipelines =
                pipelineResolver.resolvePipelines(pipelineMetricRegistry);
        final ImmutableSetMultimap<String, Pipeline> streamPipelineConnections =
                pipelineResolver.resolveStreamConnections(currentPipelines);
        final RuleMetricsConfigDto ruleMetricsConfig = ruleMetricsConfigService.get();
        return stateFactory.newState(currentPipelines, streamPipelineConnections, ruleMetricsConfig);
    }
}
