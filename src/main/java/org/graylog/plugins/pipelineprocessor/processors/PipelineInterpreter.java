/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.listeners.InterpreterListener;
import org.graylog.plugins.pipelineprocessor.processors.listeners.NoopInterpreterListener;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageCollection;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.journal.Journal;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class PipelineInterpreter implements MessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(PipelineInterpreter.class);

    public static final String GL2_PROCESSING_ERROR = "gl2_processing_error";

    private final RuleService ruleService;
    private final PipelineService pipelineService;
    private final PipelineStreamConnectionsService pipelineStreamConnectionsService;
    private final PipelineRuleParser pipelineRuleParser;
    private final Journal journal;
    private final MetricRegistry metricRegistry;
    private final ScheduledExecutorService scheduler;
    private final Meter filteredOutMessages;

    private final AtomicReference<ImmutableMap<String, Pipeline>> currentPipelines = new AtomicReference<>(ImmutableMap.of());
    private final AtomicReference<ImmutableSetMultimap<String, Pipeline>> streamPipelineConnections = new AtomicReference<>(ImmutableSetMultimap.of());

    @Inject
    public PipelineInterpreter(RuleService ruleService,
                               PipelineService pipelineService,
                               PipelineStreamConnectionsService pipelineStreamConnectionsService,
                               PipelineRuleParser pipelineRuleParser,
                               Journal journal,
                               MetricRegistry metricRegistry,
                               @Named("daemonScheduler") ScheduledExecutorService scheduler,
                               EventBus serverEventBus) {
        this.ruleService = ruleService;
        this.pipelineService = pipelineService;
        this.pipelineStreamConnectionsService = pipelineStreamConnectionsService;
        this.pipelineRuleParser = pipelineRuleParser;

        this.journal = journal;
        this.metricRegistry = metricRegistry;
        this.scheduler = scheduler;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));

        // listens to cluster wide Rule, Pipeline and pipeline stream connection changes
        serverEventBus.register(this);

        reload();
    }

    // this should not run in parallel
    private synchronized void reload() {
        // read all rules and compile them
        Map<String, Rule> ruleNameMap = Maps.newHashMap();
        for (RuleDao ruleDao : ruleService.loadAll()) {
            Rule rule;
            try {
                rule = pipelineRuleParser.parseRule(ruleDao.id(), ruleDao.source(), false);
            } catch (ParseException e) {
                rule = Rule.alwaysFalse("Failed to parse rule: " + ruleDao.id());
            }
            ruleNameMap.put(rule.name(), rule);
        }

        Map<String, Pipeline> pipelineIdMap = Maps.newHashMap();
        // read all pipelines and compile them
        for (PipelineDao pipelineDao : pipelineService.loadAll()) {
            Pipeline pipeline;
            try {
                pipeline =  pipelineRuleParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
            } catch (ParseException e) {
                pipeline = Pipeline.empty("Failed to parse pipeline" + pipelineDao.id());
            }
            pipelineIdMap.put(pipelineDao.id(), pipeline);
        }

        // resolve all rules in the stages
        pipelineIdMap.values().stream()
                .flatMap(pipeline -> {
                    log.debug("Resolving pipeline {}", pipeline.name());
                    return pipeline.stages().stream();
                })
                .forEach(stage -> {
                    final List<Rule> resolvedRules = stage.ruleReferences().stream().
                            map(ref -> {
                                Rule rule = ruleNameMap.get(ref);
                                if (rule == null) {
                                    rule = Rule.alwaysFalse("Unresolved rule " + ref);
                                }
                                log.debug("Resolved rule `{}` to {}", ref, rule);
                                return rule;
                            })
                            .collect(Collectors.toList());
                    stage.setRules(resolvedRules);
                });
        currentPipelines.set(ImmutableMap.copyOf(pipelineIdMap));

        // read all stream connections of those pipelines to allow processing messages through them
        final HashMultimap<String, Pipeline> connections = HashMultimap.create();
        for (PipelineConnections streamConnection : pipelineStreamConnectionsService.loadAll()) {
            streamConnection.pipelineIds().stream()
                    .map(pipelineIdMap::get)
                    .filter(Objects::nonNull)
                    .forEach(pipeline -> connections.put(streamConnection.streamId(), pipeline));
        }
        streamPipelineConnections.set(ImmutableSetMultimap.copyOf(connections));

    }

    /**
     * @param messages messages to process
     * @return messages to pass on to the next stage
     */
    @Override
    public Messages process(Messages messages) {
        return process(messages, new NoopInterpreterListener());
    }

    public Messages process(Messages messages, InterpreterListener interpreterListener) {
        interpreterListener.startProcessing();
        // message id + stream id
        final Set<Tuple2<String, String>> processingBlacklist = Sets.newHashSet();

        final List<Message> fullyProcessed = Lists.newArrayList();
        List<Message> toProcess = Lists.newArrayList(messages);

        while (!toProcess.isEmpty()) {
            final MessageCollection currentSet = new MessageCollection(toProcess);
            // we'll add them back below
            toProcess.clear();

            for (Message message : currentSet) {
                final String msgId = message.getId();

                // 1. for each message, determine which pipelines are supposed to be executed, based on their streams
                //    null is the default stream, the other streams are identified by their id
                final ImmutableSet<Pipeline> pipelinesToRun;

                // this makes a copy of the list!
                final Set<String> initialStreamIds = message.getStreams().stream().map(Stream::getId).collect(Collectors.toSet());

                final ImmutableSetMultimap<String, Pipeline> streamConnection = streamPipelineConnections.get();

                if (initialStreamIds.isEmpty()) {
                    if (processingBlacklist.contains(tuple(msgId, "default"))) {
                        // already processed default pipeline for this message
                        pipelinesToRun = ImmutableSet.of();
                        log.debug("[{}] already processed default stream, skipping", msgId);
                    } else {
                        // get the default stream pipeline connections for this message
                        pipelinesToRun = streamConnection.get("default");
                        interpreterListener.processDefaultStream(message, pipelinesToRun);
                        if (log.isDebugEnabled()) {
                            log.debug("[{}] running default stream pipelines: [{}]",
                                      msgId,
                                      pipelinesToRun.stream().map(Pipeline::name).toArray());
                        }
                    }
                } else {
                    // 2. if a message-stream combination has already been processed (is in the set), skip that execution
                    final Set<String> streamsIds = initialStreamIds.stream()
                            .filter(streamId -> !processingBlacklist.contains(tuple(msgId, streamId)))
                            .filter(streamConnection::containsKey)
                            .collect(Collectors.toSet());
                    pipelinesToRun = ImmutableSet.copyOf(streamsIds.stream()
                            .flatMap(streamId -> streamConnection.get(streamId).stream())
                            .collect(Collectors.toSet()));
                    interpreterListener.processStreams(message, pipelinesToRun, streamsIds);
                    log.debug("[{}] running pipelines {} for streams {}", msgId, pipelinesToRun, streamsIds);
                }

                toProcess.addAll(processForPipelines(message, msgId, pipelinesToRun.stream().map(Pipeline::id).collect(Collectors.toSet()), interpreterListener));

                boolean addedStreams = false;
                // 5. add each message-stream combination to the blacklist set
                for (Stream stream : message.getStreams()) {
                    if (!initialStreamIds.remove(stream.getId())) {
                        addedStreams = true;
                    } else {
                        // only add pre-existing streams to blacklist, this has the effect of only adding already processed streams,
                        // not newly added ones.
                        processingBlacklist.add(tuple(msgId, stream.getId()));
                    }
                }
                if (message.getFilterOut()) {
                    log.debug("[{}] marked message to be discarded. Dropping message.",
                              msgId);
                    filteredOutMessages.mark();
                    journal.markJournalOffsetCommitted(message.getJournalOffset());
                }
                // 6. go to 1 and iterate over all messages again until no more streams are being assigned
                if (!addedStreams || message.getFilterOut()) {
                    log.debug("[{}] no new streams matches or dropped message, not running again", msgId);
                    fullyProcessed.add(message);
                } else {
                    // process again, we've added a stream
                    log.debug("[{}] new streams assigned, running again for those streams", msgId);
                    toProcess.add(message);
                }
            }
        }

        interpreterListener.finishProcessing();
        // 7. return the processed messages
        return new MessageCollection(fullyProcessed);
    }

    public List<Message> processForPipelines(Message message, String msgId, Set<String> pipelines, InterpreterListener interpreterListener) {
        final ImmutableSet<Pipeline> pipelinesToRun = ImmutableSet.copyOf(pipelines.stream().map(pipelineId -> this.currentPipelines.get().get(pipelineId)).collect(Collectors.toSet()));
        final List<Message> result = new ArrayList<>();
        // record execution of pipeline in metrics
        pipelinesToRun.stream().forEach(pipeline -> metricRegistry.counter(name(Pipeline.class, pipeline.id(), "executed")).inc());

        final StageIterator stages = new StageIterator(pipelinesToRun);
        final Set<Pipeline> pipelinesToSkip = Sets.newHashSet();

        // iterate through all stages for all matching pipelines, per "stage slice" instead of per pipeline.
        // pipeline execution ordering is not guaranteed
        while (stages.hasNext()) {
            final Set<Tuple2<Stage, Pipeline>> stageSet = stages.next();
            for (Tuple2<Stage, Pipeline> pair : stageSet) {
                final Stage stage = pair.v1();
                final Pipeline pipeline = pair.v2();
                if (pipelinesToSkip.contains(pipeline)) {
                    log.debug("[{}] previous stage result prevents further processing of pipeline `{}`",
                             msgId,
                             pipeline.name());
                    continue;
                }
                metricRegistry.counter(name(Pipeline.class, pipeline.id(), "stage", String.valueOf(stage.stage()), "executed")).inc();
                interpreterListener.enterStage(stage);
                log.debug("[{}] evaluating rule conditions in stage {}: match {}",
                         msgId,
                         stage.stage(),
                         stage.matchAll() ? "all" : "either");

                // TODO the message should be decorated to allow layering changes and isolate stages
                final EvaluationContext context = new EvaluationContext(message);

                // 3. iterate over all the stages in these pipelines and execute them in order
                final ArrayList<Rule> rulesToRun = Lists.newArrayListWithCapacity(stage.getRules().size());
                boolean anyRulesMatched = false;
                for (Rule rule : stage.getRules()) {
                    interpreterListener.evaluateRule(rule, pipeline);
                    if (rule.when().evaluateBool(context)) {
                        anyRulesMatched = true;
                        countRuleExecution(rule, pipeline, stage, "matched");

                        if (context.hasEvaluationErrors()) {
                            final EvaluationContext.EvalError lastError = Iterables.getLast(context.evaluationErrors());
                            appendProcessingError(rule, message, lastError.toString());
                            interpreterListener.failEvaluateRule(rule, pipeline);
                            log.debug("Encountered evaluation error during condition, skipping rule actions: {}",
                                      lastError);
                            continue;
                        }
                        interpreterListener.satisfyRule(rule, pipeline);
                        log.debug("[{}] rule `{}` matches, scheduling to run", msgId, rule.name());
                        rulesToRun.add(rule);
                    } else {
                        countRuleExecution(rule, pipeline, stage, "not-matched");
                        interpreterListener.dissatisfyRule(rule, pipeline);
                        log.debug("[{}] rule `{}` does not match", msgId, rule.name());
                    }
                }
                RULES:
                for (Rule rule : rulesToRun) {
                    countRuleExecution(rule, pipeline, stage, "executed");
                    interpreterListener.executeRule(rule, pipeline);
                    log.debug("[{}] rule `{}` matched running actions", msgId, rule.name());
                    for (Statement statement : rule.then()) {
                        statement.evaluate(context);
                        if (context.hasEvaluationErrors()) {
                            // if the last statement resulted in an error, do not continue to execute this rules
                            final EvaluationContext.EvalError lastError = Iterables.getLast(context.evaluationErrors());
                            appendProcessingError(rule, message, lastError.toString());
                            interpreterListener.failExecuteRule(rule, pipeline);
                            log.debug("Encountered evaluation error, skipping rest of the rule: {}",
                                      lastError);
                            countRuleExecution(rule, pipeline, stage, "failed");
                            break RULES;
                        }
                    }
                }
                // stage needed to match all rule conditions to enable the next stage,
                // record that it is ok to proceed with this pipeline
                // OR
                // any rule could match, but at least one had to,
                // record that it is ok to proceed with the pipeline
                if ((stage.matchAll() && (rulesToRun.size() == stage.getRules().size()))
                        || (rulesToRun.size() > 0 && anyRulesMatched)) {
                    interpreterListener.continuePipelineExecution(pipeline, stage);
                    log.debug("[{}] stage {} for pipeline `{}` required match: {}, ok to proceed with next stage",
                             msgId, stage.stage(), pipeline.name(), stage.matchAll() ? "all" : "either");
                } else {
                    // no longer execute stages from this pipeline, the guard prevents it
                    interpreterListener.stopPipelineExecution(pipeline, stage);
                    log.debug("[{}] stage {} for pipeline `{}` required match: {}, NOT ok to proceed with next stage",
                              msgId, stage.stage(), pipeline.name(), stage.matchAll() ? "all" : "either");
                    pipelinesToSkip.add(pipeline);
                }

                // 4. after each complete stage run, merge the processing changes, stages are isolated from each other
                // TODO message changes become visible immediately for now

                // 4a. also add all new messages from the context to the toProcess work list
                Iterables.addAll(result, context.createdMessages());
                context.clearCreatedMessages();
                interpreterListener.exitStage(stage);
            }
        }

        // 7. return the processed messages
        return result;
    }

    private void countRuleExecution(Rule rule, Pipeline pipeline, Stage stage, String type) {
        metricRegistry.counter(name(Rule.class, rule.id(), type)).inc();
        metricRegistry.counter(name(Rule.class, rule.id(), pipeline.id(), String.valueOf(stage.stage()), type)).inc();
    }

    private void appendProcessingError(Rule rule, Message message, String errorString) {
        final String msg = "For rule '" + rule.name() + "': " + errorString;
        if (message.hasField(GL2_PROCESSING_ERROR)) {
            message.addField(GL2_PROCESSING_ERROR, message.getFieldAs(String.class, GL2_PROCESSING_ERROR) + "," + msg);
        } else {
            message.addField(GL2_PROCESSING_ERROR, msg);
        }
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        event.deletedRuleIds().forEach(id -> {
            log.debug("Invalidated rule {}", id);
            metricRegistry.removeMatching((name, metric) -> name.startsWith(name(Rule.class, id)));
        });
        event.updatedRuleIds().forEach(id -> {
            log.debug("Refreshing rule {}", id);
        });
        scheduler.schedule((Runnable) this::reload, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        event.deletedPipelineIds().forEach(id -> {
            log.debug("Invalidated pipeline {}", id);
            metricRegistry.removeMatching((name, metric) -> name.startsWith(name(Pipeline.class, id)));
        });
        event.updatedPipelineIds().forEach(id -> {
            log.debug("Refreshing pipeline {}", id);
        });
        scheduler.schedule((Runnable) this::reload, 0, TimeUnit.SECONDS);
    }

    @Subscribe
    public void handlePipelineConnectionChanges(PipelineConnectionsChangedEvent event) {
        log.debug("Pipeline stream connection changed: {}", event);
        scheduler.schedule((Runnable) this::reload, 0, TimeUnit.SECONDS);
    }

    public static class Descriptor implements MessageProcessor.Descriptor {
        @Override
        public String name() {
            return "Pipeline Processor";
        }

        @Override
        public String className() {
            return PipelineInterpreter.class.getCanonicalName();
        }
    }

}
