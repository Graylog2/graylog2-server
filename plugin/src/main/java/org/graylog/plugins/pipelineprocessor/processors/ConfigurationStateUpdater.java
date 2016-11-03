package org.graylog.plugins.pipelineprocessor.processors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import com.codahale.metrics.MetricRegistry;

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class ConfigurationStateUpdater {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationStateUpdater.class);

    private final RuleService ruleService;
    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService pipelineStreamConnectionsService;
    private final PipelineRuleParser pipelineRuleParser;
    private final MetricRegistry metricRegistry;
    private final ScheduledExecutorService scheduler;
    private final EventBus serverEventBus;
    /**
     * non-null if the update has successfully loaded a state
     */
    private PipelineInterpreter.State latestState;

    @Inject
    public ConfigurationStateUpdater(RuleService ruleService,
                                     PipelineService pipelineService,
                                     PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                     PipelineRuleParser pipelineRuleParser,
                                     MetricRegistry metricRegistry,
                                     @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                     EventBus serverEventBus) {
        this.ruleService = ruleService;
        this.pipelineService = pipelineService;
        this.pipelineStreamConnectionsService = pipelineStreamConnectionsService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.metricRegistry = metricRegistry;
        this.scheduler = scheduler;
        this.serverEventBus = serverEventBus;

        // listens to cluster wide Rule, Pipeline and pipeline stream connection changes
        serverEventBus.register(this);

        // eagerly propagate initial state
        serverEventBus.post(reloadAndSave());
    }

    // only the singleton instance should mutate itself, others are welcome to reload a new state, but we don't
    // currently allow direct global state updates from external sources (if you need to, send an event on the bus instead)
    private synchronized PipelineInterpreter.State reloadAndSave() {
        latestState = reload();
        return latestState;
    }

    // this should not run in parallel to avoid useless database traffic
    public synchronized PipelineInterpreter.State reload() {
        // read all rules and parse them
        Map<String, Rule> ruleNameMap = Maps.newHashMap();
        ruleService.loadAll().forEach(ruleDao -> {
            Rule rule;
            try {
                rule = pipelineRuleParser.parseRule(ruleDao.id(), ruleDao.source(), false);
            } catch (ParseException e) {
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

        return new PipelineInterpreter.State(currentPipelines, streamPipelineConnections);
    }

    /**
     * Can be used to inspect or use the current state of the pipeline system.
     * For example, the interpreter
     * @return the currently loaded state of the updater
     */
    public PipelineInterpreter.State getLatestState() {
        return latestState;
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
                        rule = rule.toBuilder().build();
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

}
