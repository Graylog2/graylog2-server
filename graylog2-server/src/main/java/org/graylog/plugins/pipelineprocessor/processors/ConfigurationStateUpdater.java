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

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.codegen.PipelineClassloader;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RuleMetricsConfigChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.tools.ToolProvider;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class ConfigurationStateUpdater {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationStateUpdater.class);

    private final RuleService ruleService;
    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService pipelineStreamConnectionsService;
    private final PipelineRuleParser pipelineRuleParser;
    private final RuleMetricsConfigService ruleMetricsConfigService;
    private final MetricRegistry metricRegistry;
    private final FunctionRegistry functionRegistry;
    private final ScheduledExecutorService scheduler;
    private final EventBus serverEventBus;
    private final PipelineInterpreter.State.Factory stateFactory;
    /**
     * non-null if the update has successfully loaded a state
     */
    private final AtomicReference<PipelineInterpreter.State> latestState = new AtomicReference<>();
    private static boolean allowCodeGeneration = false;

    @Inject
    public ConfigurationStateUpdater(RuleService ruleService,
                                     PipelineService pipelineService,
                                     PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                     PipelineRuleParser pipelineRuleParser,
                                     RuleMetricsConfigService ruleMetricsConfigService,
                                     MetricRegistry metricRegistry,
                                     FunctionRegistry functionRegistry,
                                     @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                     EventBus serverEventBus,
                                     PipelineInterpreter.State.Factory stateFactory,
                                     @Named("generate_native_code") boolean allowCodeGeneration) {
        this.ruleService = ruleService;
        this.pipelineService = pipelineService;
        this.pipelineStreamConnectionsService = pipelineStreamConnectionsService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.ruleMetricsConfigService = ruleMetricsConfigService;
        this.metricRegistry = metricRegistry;
        this.functionRegistry = functionRegistry;
        this.scheduler = scheduler;
        this.serverEventBus = serverEventBus;
        this.stateFactory = stateFactory;
        // ignore global config, never allow generating code
        setAllowCodeGeneration(false);

        // listens to cluster wide Rule, Pipeline and pipeline stream connection changes
        serverEventBus.register(this);

        reloadAndSave();
    }

    private static void setAllowCodeGeneration(Boolean allowCodeGeneration) {
        if (allowCodeGeneration && ToolProvider.getSystemJavaCompiler() == null) {
            log.warn("Your Java runtime does not have a compiler available, turning off dynamic " +
                    "code generation. Please consider running Graylog in a JDK, not a JRE, to " +
                    "avoid a performance penalty in pipeline processing.");
            allowCodeGeneration = false;
        }
        ConfigurationStateUpdater.allowCodeGeneration = allowCodeGeneration;
    }

    public static boolean isAllowCodeGeneration() {
        return allowCodeGeneration;
    }

    // only the singleton instance should mutate itself, others are welcome to reload a new state, but we don't
    // currently allow direct global state updates from external sources (if you need to, send an event on the bus instead)
    private synchronized PipelineInterpreter.State reloadAndSave() {
        // this classloader will hold all generated rule classes
        PipelineClassloader commonClassLoader = allowCodeGeneration ? new PipelineClassloader() : null;

        // read all rules and parse them
        Map<String, Rule> ruleNameMap = Maps.newHashMap();
        ruleService.loadAll().forEach(ruleDao -> {
            Rule rule;
            try {
                rule = pipelineRuleParser.parseRule(ruleDao.id(), ruleDao.source(), false, commonClassLoader);
            } catch (ParseException e) {
                log.warn("Ignoring non parseable rule <{}/{}> with errors <{}>", ruleDao.title(), ruleDao.id(), e.getErrors());
                rule = Rule.alwaysFalse("Failed to parse rule: " + ruleDao.id());
            }
            ruleNameMap.put(rule.name(), rule);
        });

        // read all pipelines and parse them
        ImmutableMap.Builder<String, Pipeline> pipelineIdMap = ImmutableMap.builder();
        pipelineService.loadAll().forEach(pipelineDao -> {
            Pipeline pipeline;
            try {
                pipeline = pipelineRuleParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
            } catch (ParseException e) {
                pipeline = Pipeline.empty("Failed to parse pipeline" + pipelineDao.id());
            }
            //noinspection ConstantConditions
            pipelineIdMap.put(pipelineDao.id(), resolvePipeline(pipeline, ruleNameMap));
        });

        final ImmutableMap<String, Pipeline> currentPipelines = pipelineIdMap.build();

        // read all stream connections of those pipelines to allow processing messages through them
        final HashMultimap<String, Pipeline> connections = HashMultimap.create();
        for (PipelineConnections streamConnection : pipelineStreamConnectionsService.loadAll()) {
            streamConnection.pipelineIds().stream()
                    .map(currentPipelines::get)
                    .filter(Objects::nonNull)
                    .forEach(pipeline -> connections.put(streamConnection.streamId(), pipeline));
        }
        ImmutableSetMultimap<String, Pipeline> streamPipelineConnections = ImmutableSetMultimap.copyOf(connections);

        final RuleMetricsConfigDto ruleMetricsConfig = ruleMetricsConfigService.get();
        final PipelineInterpreter.State newState = stateFactory.newState(currentPipelines, streamPipelineConnections, ruleMetricsConfig);
        latestState.set(newState);
        return newState;
    }


    /**
     * Can be used to inspect or use the current state of the pipeline system.
     * For example, the interpreter
     * @return the currently loaded state of the updater
     */
    public PipelineInterpreter.State getLatestState() {
        return latestState.get();
    }

    @Nonnull
    private Pipeline resolvePipeline(Pipeline pipeline, Map<String, Rule> ruleNameMap) {
        log.debug("Resolving pipeline {}", pipeline.name());

        pipeline.stages().forEach(stage -> {
            final List<Rule> resolvedRules = stage.ruleReferences().stream()
                    .map(ref -> {
                        Rule rule = ruleNameMap.get(ref);
                        if (rule == null) {
                            rule = Rule.alwaysFalse("Unresolved rule " + ref);
                        }
                        // make a copy so that the metrics match up (we don't share actual objects between stages)
                        // this also makes sure we don't accidentally share state of generated code between threads
                        rule = rule.invokableCopy(functionRegistry);
                        log.debug("Resolved rule `{}` to {}", ref, rule);
                        // include back reference to stage
                        rule.registerMetrics(metricRegistry, pipeline.id(), String.valueOf(stage.stage()));
                        return rule;
                    })
                    .collect(Collectors.toList());
            stage.setRules(resolvedRules);
            stage.setPipeline(pipeline);
            stage.registerMetrics(metricRegistry, pipeline.id());
        });

        pipeline.registerMetrics(metricRegistry);
        return pipeline;
    }

    // TODO avoid reloading everything on every change, certain changes can get away with doing less work
    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        event.deletedRuleIds().forEach(id -> {
            log.debug("Invalidated rule {}", id);
            metricRegistry.removeMatching((name, metric) -> name.startsWith(name(Rule.class, id)));
        });
        event.updatedRuleIds().forEach(id -> log.debug("Refreshing rule {}", id));
        scheduler.schedule(() -> serverEventBus.post(reloadAndSave()), 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        event.deletedPipelineIds().forEach(id -> {
            log.debug("Invalidated pipeline {}", id);
            metricRegistry.removeMatching((name, metric) -> name.startsWith(name(Pipeline.class, id)));
        });
        event.updatedPipelineIds().forEach(id -> log.debug("Refreshing pipeline {}", id));
        scheduler.schedule(() -> serverEventBus.post(reloadAndSave()), 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handlePipelineConnectionChanges(PipelineConnectionsChangedEvent event) {
        log.debug("Pipeline stream connection changed: {}", event);
        scheduler.schedule(() -> serverEventBus.post(reloadAndSave()), 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handlePipelineStateChange(PipelineInterpreter.State event) {
        log.debug("Pipeline interpreter state got updated");
    }

    @Subscribe
    public void handleRuleMetricsConfigChange(RuleMetricsConfigChangedEvent event) {
        log.debug("Rule metrics config changed: {}", event);
        scheduler.schedule(() -> serverEventBus.post(reloadAndSave()), 0, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    PipelineInterpreter.State reload() {
        return reloadAndSave();
    }
}
