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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Resolves pipelines, pipeline rules, and pipeline stream connections from database objects to pipeline AST objects.
 */
public class PipelineResolver {
    public interface Factory {
        /**
         * Creates a new instance for the given suppliers.
         *
         * @param config     the resolver params
         * @param ruleParser the pipeline rule parser to use
         * @return the new pipeline resolver instance
         */
        PipelineResolver create(PipelineResolverConfig config, PipelineRuleParser ruleParser);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PipelineResolver.class);

    private final PipelineRuleParser ruleParser;
    private final PipelineResolverConfig config;
    private final Supplier<Stream<RuleDao>> ruleDaoSupplier;
    private final Supplier<Stream<PipelineDao>> pipelineDaoSupplier;
    private final Supplier<Stream<PipelineConnections>> pipelineConnectionsSupplier;

    @Inject
    public PipelineResolver(@Assisted PipelineRuleParser ruleParser,
                            @Assisted PipelineResolverConfig config) {
        this.ruleParser = ruleParser;
        this.config = config;
        this.ruleDaoSupplier = config.rulesSupplier();
        this.pipelineDaoSupplier = config.pipelinesSupplier();
        this.pipelineConnectionsSupplier = config.pipelineConnectionsSupplier();
    }

    /**
     * Returns the pipeline resolver configuration.
     *
     * @return the configuration
     */
    public PipelineResolverConfig config() {
        return config;
    }

    /**
     * Resolve functions for the given set of {@link Pipeline} AST objects. Should only be used when programmatically
     * generating pipeline AST objects.
     *
     * @param pipelines              the pipeline AST objects
     * @param pipelineMetricRegistry the metric registry
     * @return a map of pipeline ID to pipeline instances
     */
    public ImmutableMap<String, Pipeline> resolveFunctions(Collection<Pipeline> pipelines,
                                                           PipelineMetricRegistry pipelineMetricRegistry) {
        final Map<String, Rule> ruleNameMap = resolveRules();
        final ImmutableMap.Builder<String, Pipeline> pipelineIdMap = ImmutableMap.builder();

        for (final var pipeline : pipelines) {
            final var id = requireNonNull(pipeline.id(), "pipeline ID can't be null");
            pipelineIdMap.put(id, resolvePipeline(pipelineMetricRegistry, pipeline, ruleNameMap));
        }

        return pipelineIdMap.build();
    }

    private Map<String, Rule> resolveRules() {
        // Read all rules and parse them
        final Map<String, Rule> ruleNameMap = Maps.newHashMap();

        try (final var ruleStream = ruleDaoSupplier.get()) {
            ruleStream.forEach(ruleDao -> {
                Rule rule;
                try {
                    rule = ruleParser.parseRule(ruleDao.id(), ruleDao.source(), false);
                } catch (ParseException e) {
                    LOG.warn("Ignoring non parseable rule <{}/{}> with errors <{}>", ruleDao.title(), ruleDao.id(), e.getErrors());
                    rule = Rule.alwaysFalse("Failed to parse rule: " + ruleDao.id());
                }
                ruleNameMap.put(rule.name(), rule);
            });
        }

        return ruleNameMap;
    }

    /**
     * Resolves the rule and pipeline DAO objects into AST objects.
     *
     * @return a map of pipeline ID to pipeline instances
     */
    public ImmutableMap<String, Pipeline> resolvePipelines(PipelineMetricRegistry pipelineMetricRegistry) {
        final Map<String, Rule> ruleNameMap = resolveRules();

        // Read all pipelines and parse them
        final ImmutableMap.Builder<String, Pipeline> pipelineIdMap = ImmutableMap.builder();
        try (final var pipelineStream = pipelineDaoSupplier.get()) {
            pipelineStream.forEach(pipelineDao -> {
                Pipeline pipeline;
                try {
                    pipeline = ruleParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
                } catch (ParseException e) {
                    LOG.warn("Ignoring non parseable pipeline <{}/{}> with errors <{}>", pipelineDao.title(), pipelineDao.id(), e.getErrors());
                    pipeline = Pipeline.empty("Failed to parse pipeline: " + pipelineDao.id());
                }
                //noinspection ConstantConditions
                pipelineIdMap.put(pipelineDao.id(), resolvePipeline(pipelineMetricRegistry, pipeline, ruleNameMap));
            });
        }

        return pipelineIdMap.build();
    }

    /**
     * Resolves the stream connection objects for the given pipelines map.
     *
     * @param currentPipelines the pipelines map
     * @return a multimap of stream ID to pipeline objects
     */
    public ImmutableSetMultimap<String, Pipeline> resolveStreamConnections(Map<String, Pipeline> currentPipelines) {
        // Read all stream connections of those pipelines to allow processing messages through them
        final HashMultimap<String, Pipeline> connections = HashMultimap.create();

        try (final var pipelineConnectionsStream = pipelineConnectionsSupplier.get()) {
            pipelineConnectionsStream.forEach(streamConnection -> {
                streamConnection.pipelineIds().stream()
                        .map(currentPipelines::get)
                        .filter(Objects::nonNull)
                        .forEach(pipeline -> connections.put(streamConnection.streamId(), pipeline));
            });
        }

        return ImmutableSetMultimap.copyOf(connections);
    }

    @Nonnull
    private Pipeline resolvePipeline(PipelineMetricRegistry pipelineMetricRegistry,
                                     Pipeline pipeline,
                                     Map<String, Rule> ruleNameMap) {
        LOG.debug("Resolving pipeline <{}>", pipeline.name());

        pipeline.stages().forEach(stage -> {
            final List<Rule> resolvedRules = stage.ruleReferences().stream()
                    .map(ref -> {
                        Rule rule = ruleNameMap.get(ref);
                        if (rule == null) {
                            LOG.warn("Cannot resolve rule <{}> referenced by stage #{} within pipeline <{}>",
                                    ref, stage.stage(), pipeline.id());
                            rule = Rule.alwaysFalse("Unresolved rule " + ref);
                        }
                        // make a copy so that the metrics match up (we don't share actual objects between stages)
                        rule = rule.copy();
                        LOG.debug("Resolved rule <{}> to <{}>", ref, rule);
                        // include back reference to stage
                        rule.registerMetrics(pipelineMetricRegistry, pipeline.id(), stage.stage());
                        return rule;
                    })
                    .collect(Collectors.toList());
            stage.setRules(resolvedRules);
            stage.setPipeline(pipeline);
            stage.registerMetrics(pipelineMetricRegistry, pipeline.id());
        });

        pipeline.registerMetrics(pipelineMetricRegistry);

        return pipeline;
    }
}
