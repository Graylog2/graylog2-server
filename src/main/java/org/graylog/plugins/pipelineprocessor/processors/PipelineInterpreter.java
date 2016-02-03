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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.statements.Statement;
import org.graylog.plugins.pipelineprocessor.db.PipelineSourceService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamAssignmentService;
import org.graylog.plugins.pipelineprocessor.db.RuleSourceService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamAssignment;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageCollection;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.journal.Journal;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.cache.CacheLoader.asyncReloading;
import static org.jooq.lambda.tuple.Tuple.tuple;

public class PipelineInterpreter implements MessageProcessor {
    private static final Logger log = LoggerFactory.getLogger(PipelineInterpreter.class);

    private final PipelineSourceService pipelineSourceService;
    private final PipelineRuleParser pipelineRuleParser;
    private final Journal journal;
    private final Meter filteredOutMessages;
    private final LoadingCache<String, Rule> ruleCache;
    private final ListeningScheduledExecutorService scheduledExecutorService;

    private final AtomicReference<ImmutableMap<String, Pipeline>> currentPipelines = new AtomicReference<>();
    private final AtomicReference<ImmutableSetMultimap<String, Pipeline>> streamPipelineAssignments = new AtomicReference<>(ImmutableSetMultimap.of());

    @Inject
    public PipelineInterpreter(RuleSourceService ruleSourceService,
                               PipelineSourceService pipelineSourceService,
                               PipelineStreamAssignmentService pipelineStreamAssignmentService,
                               PipelineRuleParser pipelineRuleParser,
                               Journal journal,
                               MetricRegistry metricRegistry,
                               @Named("daemonScheduler") ScheduledExecutorService scheduledExecutorService,
                               @ClusterEventBus EventBus clusterBus) {
        this.pipelineSourceService = pipelineSourceService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.journal = journal;
        this.scheduledExecutorService = MoreExecutors.listeningDecorator(scheduledExecutorService);
        this.filteredOutMessages = metricRegistry.meter(name(ProcessBufferProcessor.class, "filteredOutMessages"));
        clusterBus.register(this);
        ruleCache = CacheBuilder.newBuilder()
                .build(asyncReloading(new RuleLoader(ruleSourceService, pipelineRuleParser), scheduledExecutorService));

        // prime the cache with all presently stored rules
        try {
            final List<String> ruleIds = ruleSourceService.loadAll().stream().map(RuleSource::id).collect(Collectors.toList());
            log.info("Compiling {} processing rules", ruleIds.size());
            // TODO this sucks, because it is completely async and we can't use listenable futures to trigger the pipeline updates
            ruleCache.getAll(ruleIds);
            triggerPipelineUpdate();
        } catch (ExecutionException ignored) {
        }
    }

    /**
     * @param messages messages to process
     * @return messages to pass on to the next stage
     */
    @Override
    public Messages process(Messages messages) {
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

                final ImmutableSetMultimap<String, Pipeline> streamAssignment = streamPipelineAssignments.get();

                if (initialStreamIds.isEmpty()) {
                    if (processingBlacklist.contains(tuple(msgId, "default"))) {
                        // already processed default pipeline for this message
                        pipelinesToRun = ImmutableSet.of();
                        log.info("[{}] already processed default stream, skipping", msgId);
                    } else {
                        // get the default stream pipeline assignments for this message
                        pipelinesToRun = streamAssignment.get("default");
                        log.info("[{}] running default stream pipelines: [{}]",
                                 msgId,
                                 pipelinesToRun.stream().map(Pipeline::name).toArray());
                    }
                } else {
                    // 2. if a message-stream combination has already been processed (is in the set), skip that execution
                    final Set<String> streamsIds = initialStreamIds.stream()
                            .filter(streamId -> !processingBlacklist.contains(tuple(msgId, streamId)))
                            .filter(streamAssignment::containsKey)
                            .collect(Collectors.toSet());
                    pipelinesToRun = ImmutableSet.copyOf(streamsIds.stream()
                            .flatMap(streamId -> streamAssignment.get(streamId).stream())
                            .collect(Collectors.toSet()));
                    log.info("[{}] running pipelines {} for streams {}", msgId, pipelinesToRun, streamsIds);
                }

                final StageIterator stages = new StageIterator(pipelinesToRun);
                final Set<Pipeline> pipelinesToProceedWith = Sets.newHashSet();

                // iterate through all stages for all matching pipelines, per "stage slice" instead of per pipeline.
                // pipeline execution ordering is not guaranteed
                while (stages.hasNext()) {
                    final Set<Tuple2<Stage, Pipeline>> stageSet = stages.next();
                    for (Tuple2<Stage, Pipeline> pair : stageSet) {
                        final Stage stage = pair.v1();
                        final Pipeline pipeline = pair.v2();
                        if (!pipelinesToProceedWith.isEmpty() &&
                                !pipelinesToProceedWith.contains(pipeline)) {
                            log.info("[{}] previous stage result prevents further processing of pipeline `{}`",
                                     msgId,
                                     pipeline.name());
                            continue;
                        }
                        log.info("[{}] evaluating rule conditions in stage {}: match {}",
                                 msgId,
                                 stage.stage(),
                                 stage.matchAll() ? "all" : "either");

                        // TODO the message should be decorated to allow layering changes and isolate stages
                        final EvaluationContext context = new EvaluationContext(message);

                        // 3. iterate over all the stages in these pipelines and execute them in order
                        final ArrayList<Rule> rulesToRun = Lists.newArrayListWithCapacity(stage.getRules().size());
                        for (Rule rule : stage.getRules()) {
                            if (rule.when().evaluateBool(context)) {
                                log.info("[{}] rule `{}` matches, scheduling to run", msgId, rule.name());
                                rulesToRun.add(rule);
                            } else {
                                log.info("[{}] rule `{}` does not match", msgId, rule.name());
                            }
                        }
                        for (Rule rule : rulesToRun) {
                            log.info("[{}] rule `{}` matched running actions", msgId, rule.name());
                            for (Statement statement : rule.then()) {
                                statement.evaluate(context);
                            }
                        }
                        // stage needed to match all rule conditions to enable the next stage,
                        // record that it is ok to proceed with this pipeline
                        // OR
                        // any rule could match, but at least one had to,
                        // record that it is ok to proceed with the pipeline
                        if ((stage.matchAll() && (rulesToRun.size() == stage.getRules().size()))
                                || (rulesToRun.size() > 0)) {
                            log.info("[{}] stage for pipeline `{}` required match: {}, ok to proceed with next stage",
                                     msgId, pipeline.name(), stage.matchAll() ? "all" : "either");
                            pipelinesToProceedWith.add(pipeline);
                        }

                        // 4. after each complete stage run, merge the processing changes, stages are isolated from each other
                        // TODO message changes become visible immediately for now

                        // 4a. also add all new messages from the context to the toProcess work list
                        Iterables.addAll(toProcess, context.createdMessages());
                        context.clearCreatedMessages();
                    }

                }
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
                    log.info("[{}] no new streams matches or dropped message, not running again", msgId);
                    fullyProcessed.add(message);
                } else {
                    // process again, we've added a stream
                    log.info("[{}] new streams assigned, running again for those streams", msgId);
                    toProcess.add(message);
                }
            }
        }
        // 7. return the processed messages
        return new MessageCollection(fullyProcessed);
    }

    @Subscribe
    public void handleRuleChanges(RulesChangedEvent event) {
        event.deletedRuleIds().forEach(id -> {
            ruleCache.invalidate(id);
            log.info("Invalidated rule {}", id);
        });
        event.updatedRuleIds().forEach(id -> {
            ruleCache.refresh(id);
            log.info("Refreshing rule {}", id);
        });

        triggerPipelineUpdate();
    }

    @Subscribe
    public void handlePipelineChanges(PipelinesChangedEvent event) {
        event.deletedPipelineIds().forEach(id -> {
            log.info("Invalidated pipeline {}", id);
        });
        event.updatedPipelineIds().forEach(id -> {
            log.info("Refreshing pipeline {}", id);
        });

        triggerPipelineUpdate();
    }

    @Subscribe
    public void handlePipelineAssignmentChanges(PipelineStreamAssignment assignment) {
        // rebuild the stream -> pipelines multimap
        // default stream is represented as "default" in the map
        final String streamId = assignment.streamId();
        final Set<String> pipelineIds = assignment.pipelineIds();

        final ImmutableSetMultimap<String, Pipeline> multimap = streamPipelineAssignments.get();
        final ImmutableMap<String, Pipeline> pipelines = currentPipelines.get();

        // set the new per-stream mapping
        final HashMultimap<String, Pipeline> newMap = HashMultimap.create(multimap);
        newMap.removeAll(streamId);
        pipelineIds.stream().map(pipelines::get).forEach(pipeline -> newMap.put(streamId, pipeline));

        streamPipelineAssignments.set(ImmutableSetMultimap.copyOf(newMap));
    }

    private void triggerPipelineUpdate() {
        Futures.addCallback(
                scheduledExecutorService.schedule(new PipelineResolver(), 500, TimeUnit.MILLISECONDS),
                new FutureCallback<ImmutableSet<Pipeline>>() {
                    @Override
                    public void onSuccess(@Nullable ImmutableSet<Pipeline> result) {
                        // TODO how do we deal with concurrent updates? canceling earlier attempts?
                        if (result == null) {
                            currentPipelines.set(ImmutableMap.of());
                        } else {
                            currentPipelines.set(Maps.uniqueIndex(result, Pipeline::id));
                        }
                    }
                    @Override
                    public void onFailure(Throwable t) {
                        // do not touch the existing pipeline configuration
                        log.error("Unable to update pipeline processor", t);
                    }
                });
    }

    private static class RuleLoader extends CacheLoader<String, Rule> {
        private final RuleSourceService ruleSourceService;
        private final PipelineRuleParser pipelineRuleParser;

        public RuleLoader(RuleSourceService ruleSourceService, PipelineRuleParser pipelineRuleParser) {
            this.ruleSourceService = ruleSourceService;
            this.pipelineRuleParser = pipelineRuleParser;
        }

        @Override
        public Map<String, Rule> loadAll(Iterable<? extends String> keys) throws Exception {
            final Map<String, Rule> all = Maps.newHashMap();
            final HashSet<String> keysToLoad = Sets.newHashSet(keys);
            for (RuleSource ruleSource : ruleSourceService.loadAll()) {
                if (!keysToLoad.isEmpty()) {
                    if (!keysToLoad.contains(ruleSource.id())) {
                        continue;
                    }
                }
                try {
                    all.put(ruleSource.id(), pipelineRuleParser.parseRule(ruleSource.source()));
                } catch (ParseException e) {
                    log.error("Unable to parse rule: " + e.getMessage());
                    all.put(ruleSource.id(), Rule.alwaysFalse("Failed to parse rule: " + ruleSource.id()));
                }
            }
            return all;
        }

        @Override
        public Rule load(@Nullable String ruleId) throws Exception {
            final RuleSource ruleSource = ruleSourceService.load(ruleId);
            try {
                return pipelineRuleParser.parseRule(ruleSource.source());
            } catch (ParseException e) {
                log.error("Unable to parse rule: " + e.getMessage());
                // return dummy rule
                return Rule.alwaysFalse("Failed to parse rule: " + ruleSource.id());
            }
        }
    }

    private class PipelineResolver implements Callable<ImmutableSet<Pipeline>> {
        private final Logger log = LoggerFactory.getLogger(PipelineResolver.class);


        @Override
        public ImmutableSet<Pipeline> call() throws Exception {
            log.info("Updating pipeline processor after rule/pipeline update");

            final Collection<PipelineSource> allPipelineSources = pipelineSourceService.loadAll();
            log.info("Found {} pipelines to resolve", allPipelineSources.size());

            // compile all pipelines
            Set<Pipeline> pipelines = Sets.newHashSetWithExpectedSize(allPipelineSources.size());
            for (PipelineSource source : allPipelineSources) {
                try {
                    final Pipeline pipeline = pipelineRuleParser.parsePipeline(source);
                    pipelines.add(pipeline);
                    log.info("Parsed pipeline {} with {} stages", pipeline.name(), pipeline.stages().size());
                } catch (ParseException e) {
                    log.warn("Unable to compile pipeline {}: {}", source.title(), e);
                }
            }

            // change the rule cache to be able to quickly look up rules by name
            final Map<String, Rule> nameRuleMap =
                    Seq.toMap(Seq.seq(ruleCache.asMap())
                                      .map(entry -> tuple(entry.v2().name(), entry.v2())));

            // resolve all rules
            pipelines.stream()
                    .flatMap(pipeline -> {
                        log.info("Resolving pipeline {}", pipeline.name());
                        return pipeline.stages().stream();
                    })
                    .forEach(stage -> {
                        final List<Rule> resolvedRules = stage.ruleReferences().stream().
                                map(ref -> {
                                    Rule rule = nameRuleMap.get(ref);
                                    if (rule == null) {
                                        rule = Rule.alwaysFalse("Unresolved rule " + ref);
                                    }
                                    log.info("Resolved rule `{}` to {}", ref, rule);
                                    return rule;
                                })
                                .collect(Collectors.toList());
                        stage.setRules(resolvedRules);
                    });

            return ImmutableSet.copyOf(pipelines);
        }
    }

}
