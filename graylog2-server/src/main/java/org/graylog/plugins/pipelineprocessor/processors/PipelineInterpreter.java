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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.codegen.GeneratedRule;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.processors.listeners.InterpreterListener;
import org.graylog.plugins.pipelineprocessor.processors.listeners.NoopInterpreterListener;
import org.graylog.plugins.pipelineprocessor.processors.listeners.RuleMetricsListener;
import org.graylog2.metrics.CacheStatsSet;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageCollection;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.metrics.MetricUtils;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class PipelineInterpreter implements MessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(PipelineInterpreter.class);

    private final MessageQueueAcknowledger messageQueueAcknowledger;
    private final Meter filteredOutMessages;
    private final Timer executionTime;
    private final MetricRegistry metricRegistry;
    private final ConfigurationStateUpdater stateUpdater;

    @Inject
    public PipelineInterpreter(MessageQueueAcknowledger messageQueueAcknowledger,
                               MetricRegistry metricRegistry,
                               ConfigurationStateUpdater stateUpdater) {

        this.messageQueueAcknowledger = messageQueueAcknowledger;
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        this.executionTime = metricRegistry.timer(name(PipelineInterpreter.class, "executionTime"));
        this.metricRegistry = metricRegistry;
        this.stateUpdater = stateUpdater;
    }

    /**
     * @param messages messages to process
     * @return messages to pass on to the next stage
     */
    @Override
    public Messages process(Messages messages) {
        try (Timer.Context ignored = executionTime.time()) {
            final State latestState = stateUpdater.getLatestState();
            if (latestState.enableRuleMetrics()) {
                return process(messages, new RuleMetricsListener(metricRegistry), latestState);
            }
            return process(messages, new NoopInterpreterListener(), latestState);
        }
    }

    /**
     * Evaluates all pipelines that apply to the given messages, based on the current stream routing
     * of the messages.
     *
     * The processing loops on each single message (passed in or created by pipelines) until the set
     * of streams does not change anymore. No cycle detection is performed.
     *
     * @param messages            the messages to process through the pipelines
     * @param interpreterListener a listener which gets called for each processing stage (e.g. to
     *                            trace execution)
     * @param state               the pipeline/stage/rule/stream connection state to use during
     *                            processing
     * @return the processed messages
     */
    public Messages process(Messages messages, InterpreterListener interpreterListener, State state) {
        interpreterListener.startProcessing();
        // message id + stream id
        final Set<Tuple2<String, String>> processingBlacklist = Sets.newHashSet();

        final List<Message> toProcess = Lists.newArrayList(messages);
        final List<Message> fullyProcessed = Lists.newArrayListWithExpectedSize(toProcess.size());

        while (!toProcess.isEmpty()) {
            final MessageCollection currentSet = new MessageCollection(toProcess);
            // we'll add them back below
            toProcess.clear();

            for (Message message : currentSet) {
                final String msgId = message.getId();

                // this makes a copy of the list, which is mutated later in updateStreamBlacklist
                // it serves as a worklist, to keep track of which <msg, stream> tuples need to be re-run again
                final Set<String> initialStreamIds = message.getStreams().stream().map(Stream::getId).collect(Collectors.toSet());

                final ImmutableSet<Pipeline> pipelinesToRun = selectPipelines(interpreterListener,
                        processingBlacklist,
                        message,
                        initialStreamIds,
                        state.getStreamPipelineConnections());

                toProcess.addAll(processForResolvedPipelines(message, msgId, pipelinesToRun, interpreterListener, state));

                // add each processed message-stream combination to the blacklist set and figure out if the processing
                // has added a stream to the message, in which case we need to cycle and determine whether to process
                // its pipeline connections, too
                boolean addedStreams = updateStreamBlacklist(processingBlacklist,
                        message,
                        initialStreamIds);
                potentiallyDropFilteredMessage(message);

                // go to 1 and iterate over all messages again until no more streams are being assigned
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

    private void potentiallyDropFilteredMessage(Message message) {
        if (message.getFilterOut()) {
            log.debug("[{}] marked message to be discarded. Dropping message.", message.getId());
            filteredOutMessages.mark();
            messageQueueAcknowledger.acknowledge(message);
        }
    }

    // given the initial streams the message was on before the processing and its current state, update the set of
    // <msgid, stream> that should not be run again (which prevents re-running pipelines over and over again)
    private boolean updateStreamBlacklist(Set<Tuple2<String, String>> processingBlacklist,
                                          Message message,
                                          Set<String> initialStreamIds) {
        boolean addedStreams = false;
        for (Stream stream : message.getStreams()) {
            if (!initialStreamIds.remove(stream.getId())) {
                addedStreams = true;
            } else {
                // only add pre-existing streams to blacklist, this has the effect of only adding already processed streams,
                // not newly added ones.
                processingBlacklist.add(tuple(message.getId(), stream.getId()));
            }
        }
        return addedStreams;
    }

    // determine which pipelines should be executed give the stream-pipeline connections and the current message
    // the initialStreamIds are not mutated, but are begin passed for efficiency, as they are being used later in #process()
    private ImmutableSet<Pipeline> selectPipelines(InterpreterListener interpreterListener,
                                                   Set<Tuple2<String, String>> processingBlacklist,
                                                   Message message,
                                                   Set<String> initialStreamIds,
                                                   ImmutableSetMultimap<String, Pipeline> streamConnection) {
        final String msgId = message.getId();

        // if a message-stream combination has already been processed (is in the set), skip that execution
        final Set<String> streamsIds = initialStreamIds.stream()
                .filter(streamId -> !processingBlacklist.contains(tuple(msgId, streamId)))
                .filter(streamConnection::containsKey)
                .collect(Collectors.toSet());
        final ImmutableSet<Pipeline> pipelinesToRun = streamsIds.stream()
                .flatMap(streamId -> streamConnection.get(streamId).stream())
                .collect(ImmutableSet.toImmutableSet());
        interpreterListener.processStreams(message, pipelinesToRun, streamsIds);
        log.debug("[{}] running pipelines {} for streams {}", msgId, pipelinesToRun, streamsIds);
        return pipelinesToRun;
    }

    /**
     * Given a set of pipeline ids, process the given message according to the passed state.
     *
     * This method returns the list of messages produced by the configuration in state, it does not
     * look at the database or any other external resource besides what is being passed as
     * parameters.
     *
     * This can be used to simulate pipelines without having to store them in the database.
     *
     * @param message             the message to process
     * @param pipelineIds         the ids of the pipelines to resolve and run the message through
     * @param interpreterListener the listener tracing the execution
     * @param state               the pipeline/stage/rule state to interpret
     * @return the list of messages created during the interpreter run
     */
    public List<Message> processForPipelines(Message message,
                                             Set<String> pipelineIds,
                                             InterpreterListener interpreterListener,
                                             State state) {
        final Map<String, Pipeline> currentPipelines = state.getCurrentPipelines();
        final ImmutableSet<Pipeline> pipelinesToRun = pipelineIds.stream()
                .map(currentPipelines::get)
                .filter(Objects::nonNull)
                .collect(ImmutableSet.toImmutableSet());

        return processForResolvedPipelines(message, message.getId(), pipelinesToRun, interpreterListener, state);
    }

    private List<Message> processForResolvedPipelines(Message message,
                                                      String msgId,
                                                      Set<Pipeline> pipelines,
                                                      InterpreterListener interpreterListener,
                                                      State state) {
        final List<Message> result = new ArrayList<>();
        // record execution of pipeline in metrics
        pipelines.forEach(Pipeline::markExecution);

        final StageIterator stages = state.getStageIterator(pipelines);
        final Set<Pipeline> pipelinesToSkip = Sets.newHashSet();

        // iterate through all stages for all matching pipelines, per "stage slice" instead of per pipeline.
        // pipeline execution ordering is not guaranteed
        while (stages.hasNext()) {
            final List<Stage> stageSet = stages.next();
            for (final Stage stage : stageSet)
                evaluateStage(stage, message, msgId, result, pipelinesToSkip, interpreterListener);
        }

        // 7. return the processed messages
        return result;
    }

    private void evaluateStage(Stage stage,
                               Message message,
                               String msgId,
                               List<Message> result,
                               Set<Pipeline> pipelinesToSkip,
                               InterpreterListener interpreterListener) {
        final Pipeline pipeline = stage.getPipeline();
        if (pipelinesToSkip.contains(pipeline)) {
            log.debug("[{}] previous stage result prevents further processing of pipeline `{}`",
                    msgId,
                    pipeline.name());
            return;
        }
        stage.markExecution();
        interpreterListener.enterStage(stage);
        log.debug("[{}] evaluating rule conditions in stage {}: match {}",
                msgId,
                stage.stage(),
                stage.matchAll() ? "all" : "either");

        // TODO the message should be decorated to allow layering changes and isolate stages
        final EvaluationContext context = new EvaluationContext(message);

        // 3. iterate over all the stages in these pipelines and execute them in order
        final List<Rule> stageRules = stage.getRules();
        final List<Rule> rulesToRun = new ArrayList<>(stageRules.size());
        boolean anyRulesMatched = stageRules.isEmpty(); // If there are no rules, we can simply continue to the next stage
        boolean allRulesMatched = true;
        for (Rule rule : stageRules) {
            try {
                final boolean ruleCondition = evaluateRuleCondition(rule, message, msgId, pipeline, context, rulesToRun, interpreterListener);
                anyRulesMatched |= ruleCondition;
                allRulesMatched &= ruleCondition;
            } catch(Exception e) {
                log.error("This exception was caused by \"{}\"/{} containing message: {}", rule.name(), rule.id(), message);
                throw e;
            }
        }

        for (Rule rule : rulesToRun) {
            if (!executeRuleActions(rule, message, msgId, pipeline, context, interpreterListener)) {
                // if any of the rules raise an error, skip the rest of the rules
                break;
            }
        }
        // stage needed to match all rule conditions to enable the next stage,
        // record that it is ok to proceed with this pipeline
        // OR
        // any rule could match, but at least one had to,
        // record that it is ok to proceed with the pipeline
        final boolean matchAllSuccess = stage.matchAll() && allRulesMatched;
        final boolean matchEitherSuccess = !stage.matchAll() && anyRulesMatched;
        if (matchAllSuccess || matchEitherSuccess) {
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

    private boolean executeRuleActions(Rule rule,
                                       Message message,
                                       String msgId,
                                       Pipeline pipeline,
                                       EvaluationContext context,
                                       InterpreterListener interpreterListener) {
        rule.markExecution();
        interpreterListener.executeRule(rule, pipeline);
        try {
            log.debug("[{}] rule `{}` matched running actions", msgId, rule.name());
            final GeneratedRule generatedRule = rule.generatedRule();
            if (generatedRule != null) {
                try {
                    generatedRule.then(context);
                    return true;
                } catch (Exception ignored) {
                    final EvaluationContext.EvalError lastError = Iterables.getLast(context.evaluationErrors());
                    appendProcessingError(rule, message, lastError.toString());
                    log.debug("Encountered evaluation error, skipping rest of the rule: {}", lastError);
                    rule.markFailure();
                    return false;
                }
            } else {
                if (ConfigurationStateUpdater.isAllowCodeGeneration()) {
                    throw new IllegalStateException("Should have generated code and not interpreted the tree");
                }
                for (Statement statement : rule.then()) {
                    if (!evaluateStatement(message, interpreterListener, pipeline, context, rule, statement)) {
                        // statement raised an error, skip the rest of the rule
                        return false;
                    }
                }
            }
            return true;
        } finally {
            interpreterListener.finishExecuteRule(rule, pipeline);
        }
    }

    private boolean evaluateStatement(Message message,
                                      InterpreterListener interpreterListener,
                                      Pipeline pipeline,
                                      EvaluationContext context, Rule rule, Statement statement) {
        statement.evaluate(context);
        if (context.hasEvaluationErrors()) {
            // if the last statement resulted in an error, do not continue to execute this rules
            final EvaluationContext.EvalError lastError = Iterables.getLast(context.evaluationErrors());
            appendProcessingError(rule, message, lastError.toString());
            interpreterListener.failExecuteRule(rule, pipeline);
            log.debug("Encountered evaluation error, skipping rest of the rule: {}",
                    lastError);
            rule.markFailure();
            return false;
        }
        return true;
    }

    private boolean evaluateRuleCondition(Rule rule,
                                          Message message,
                                          String msgId,
                                          Pipeline pipeline,
                                          EvaluationContext context,
                                          List<Rule> rulesToRun, InterpreterListener interpreterListener) {
        interpreterListener.evaluateRule(rule, pipeline);
        final GeneratedRule generatedRule = rule.generatedRule();
        boolean matched = generatedRule != null ? generatedRule.when(context) : rule.when().evaluateBool(context);
        if (matched) {
            rule.markMatch();

            if (context.hasEvaluationErrors()) {
                final EvaluationContext.EvalError lastError = Iterables.getLast(context.evaluationErrors());
                appendProcessingError(rule, message, lastError.toString());
                interpreterListener.failEvaluateRule(rule, pipeline);
                log.debug("Encountered evaluation error during condition, skipping rule actions: {}",
                        lastError);
                return false;
            }
            interpreterListener.satisfyRule(rule, pipeline);
            log.debug("[{}] rule `{}` matches, scheduling to run", msgId, rule.name());
            rulesToRun.add(rule);
            return true;
        } else {
            rule.markNonMatch();
            interpreterListener.dissatisfyRule(rule, pipeline);
            log.debug("[{}] rule `{}` does not match", msgId, rule.name());
        }
        return false;
    }

    private void appendProcessingError(Rule rule, Message message, String errorString) {
        final String msg = "For rule '" + rule.name() + "': " + errorString;
        if (message.hasField(Message.FIELD_GL2_PROCESSING_ERROR)) {
            message.addField(Message.FIELD_GL2_PROCESSING_ERROR, message.getFieldAs(String.class, Message.FIELD_GL2_PROCESSING_ERROR) + "," + msg);
        } else {
            message.addField(Message.FIELD_GL2_PROCESSING_ERROR, msg);
        }
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

    public static class State {
        private static final Logger LOG = LoggerFactory.getLogger(State.class);

        private final ImmutableMap<String, Pipeline> currentPipelines;
        private final ImmutableSetMultimap<String, Pipeline> streamPipelineConnections;
        private final LoadingCache<Set<Pipeline>, StageIterator.Configuration> cache;
        private final boolean cachedIterators;
        private final RuleMetricsConfigDto ruleMetricsConfig;

        @AssistedInject
        public State(@Assisted ImmutableMap<String, Pipeline> currentPipelines,
                     @Assisted ImmutableSetMultimap<String, Pipeline> streamPipelineConnections,
                     @Assisted RuleMetricsConfigDto ruleMetricsConfig,
                     MetricRegistry metricRegistry,
                     @Named("processbuffer_processors") int processorCount,
                     @Named("cached_stageiterators") boolean cachedIterators) {
            this.currentPipelines = currentPipelines;
            this.streamPipelineConnections = streamPipelineConnections;
            this.cachedIterators = cachedIterators;
            this.ruleMetricsConfig = ruleMetricsConfig;

            cache = CacheBuilder.newBuilder()
                    .concurrencyLevel(processorCount)
                    .recordStats()
                    .build(new CacheLoader<Set<Pipeline>, StageIterator.Configuration>() {
                        @Override
                        public StageIterator.Configuration load(@Nonnull Set<Pipeline> pipelines) throws Exception {
                            return new StageIterator.Configuration(pipelines);
                        }
                    });

            // we have to remove the metrics, because otherwise we leak references to the cache (and the register call with throw)
            metricRegistry.removeMatching((name, metric) -> name.startsWith(name(PipelineInterpreter.class, "stage-cache")));
            MetricUtils.safelyRegisterAll(metricRegistry, new CacheStatsSet(name(PipelineInterpreter.class, "stage-cache"), cache));
        }

        public ImmutableMap<String, Pipeline> getCurrentPipelines() {
            return currentPipelines;
        }

        public ImmutableSetMultimap<String, Pipeline> getStreamPipelineConnections() {
            return streamPipelineConnections;
        }

        public boolean enableRuleMetrics() {
            return ruleMetricsConfig.metricsEnabled();
        }

        public StageIterator getStageIterator(Set<Pipeline> pipelines) {
            try {
                if (cachedIterators) {
                    return new StageIterator(cache.get(pipelines));
                } else {
                    return new StageIterator(pipelines);
                }
            } catch (ExecutionException e) {
                LOG.error("Unable to get StageIterator from cache, this should not happen.", ExceptionUtils.getRootCause(e));
                return new StageIterator(pipelines);
            }
        }


        public interface Factory {
            State newState(ImmutableMap<String, Pipeline> currentPipelines,
                           ImmutableSetMultimap<String, Pipeline> streamPipelineConnections,
                           RuleMetricsConfigDto ruleMetricsConfig);
        }
    }
}
