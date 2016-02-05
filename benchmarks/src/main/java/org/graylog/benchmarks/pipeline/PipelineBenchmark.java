/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.graylog.benchmarks.pipeline;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.db.PipelineSourceService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamAssignmentService;
import org.graylog.plugins.pipelineprocessor.db.RuleSourceService;
import org.graylog.plugins.pipelineprocessor.functions.StringCoercion;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamAssignment;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
import org.graylog2.filters.ExtractorFilter;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.RulesFilter;
import org.graylog2.filters.StaticFieldFilter;
import org.graylog2.filters.StreamMatcherFilter;
import org.graylog2.inputs.InputService;
import org.graylog2.messageprocessors.MessageFilterChainProcessor;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.rules.DroolsEngine;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.stats.ThroughputStats;
import org.graylog2.streams.StreamFaultManager;
import org.graylog2.streams.StreamMetrics;
import org.graylog2.streams.StreamRouter;
import org.graylog2.streams.StreamRouterEngine;
import org.graylog2.streams.StreamService;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("Duplicates")
public class PipelineBenchmark {

    @State(Scope.Benchmark)
    public static class InterpreterState {

        private final PipelineInterpreter interpreter;

        public InterpreterState() {
            final RuleSourceService ruleSourceService = mock(RuleSourceService.class);
            when(ruleSourceService.loadAll()).thenReturn(Collections.singleton(
                    RuleSource.create("abc",
                                      "title",
                                      "description",
                                      "rule \"add\"\n" +
                                              "when string(message.`message`) == \"original message\"\n" +
                                              "then\n" +
                                              "  set_field(\"field\", \"derived message\");\n" +
                                              "end",
                                      Tools.nowUTC(),
                                      null)
            ));

            final PipelineSourceService pipelineSourceService = mock(PipelineSourceService.class);
            when(pipelineSourceService.loadAll()).thenReturn(Collections.singleton(
                    PipelineSource.create("cde", "title", "description",
                                          "pipeline \"pipeline\"\n" +
                                                  "stage 0 match all\n" +
                                                  "    rule \"add\";\n" +
                                                  "end\n",
                                          Tools.nowUTC(),
                                          null)
            ));

            final PipelineStreamAssignmentService pipelineStreamAssignmentService = mock(PipelineStreamAssignmentService.class);
            final PipelineStreamAssignment pipelineStreamAssignment = PipelineStreamAssignment.create(null,
                                                                                                      "default",
                                                                                                      newHashSet("cde"));
            when(pipelineStreamAssignmentService.loadAll()).thenReturn(
                    newHashSet(pipelineStreamAssignment)
            );

            final Map<String, Function<?>> functions = Maps.newHashMap();
            functions.put(SetField.NAME, new SetField());
            functions.put(StringCoercion.NAME, new StringCoercion());

            final PipelineRuleParser parser = setupParser(functions);

            interpreter = new PipelineInterpreter(
                    ruleSourceService,
                    pipelineSourceService,
                    pipelineStreamAssignmentService,
                    parser,
                    mock(Journal.class),
                    mock(MetricRegistry.class),
                    Executors.newScheduledThreadPool(1),
                    mock(EventBus.class)
            );
        }

        private PipelineRuleParser setupParser(Map<String, org.graylog.plugins.pipelineprocessor.ast.functions.Function<?>> functions) {
            final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
            return new PipelineRuleParser(functionRegistry);
        }

    }

    @Benchmark
    public void testPipeline(final InterpreterState state) {
        Message msg = new Message("original message", "test", Tools.nowUTC());
        state.interpreter.process(msg);
    }

    @State(Scope.Benchmark)
    public static class MessageFilterState {

        @Param("false")
        public boolean noDrools;

        public MessageFilterChainProcessor filterChain;

        @Setup
        public void setup() {
            try {
                // common objects
                final Set<MessageFilter> filters = Sets.newHashSet();
                final LocalMetricRegistry metricRegistry = new LocalMetricRegistry();
                final ServerStatus serverStatus = mock(ServerStatus.class);
                when(serverStatus.getDetailedMessageRecordingStrategy()).thenReturn(ServerStatus.MessageDetailRecordingStrategy.NEVER);

                final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(true).build());

                final InputService inputService = mock(InputService.class);
                // extractors for the single input we are pretending to have
                when(inputService.find(anyString())).thenReturn(null);

                when(inputService.getExtractors(any())).thenReturn(Lists.newArrayList());

                // static fields
                Map.Entry<String, String> staticField = new HashMap.SimpleEntry<>("field", "derived message");
                when(inputService.getStaticFields(any())).thenReturn(ImmutableList.of(staticField));

                // stream router
                final StreamService streamService = mock(StreamService.class);
                final StreamRouterEngine.Factory engineFactory = mock(StreamRouterEngine.Factory.class);
                final StreamMetrics streamMetrics = new StreamMetrics(metricRegistry);
                final StreamFaultManager streamFaultManager = new StreamFaultManager(
                        new Configuration(),
                        streamMetrics,
                        mock(NotificationService.class),
                        streamService
                );
                ExecutorService daemonExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build());
                when(engineFactory.create(any(), any())).thenReturn(
                        new StreamRouterEngine(Collections.emptyList(),
                                               daemonExecutor,
                                               streamFaultManager,
                                               streamMetrics)
                );
                final StreamRouter streamRouter = new StreamRouter(streamService,
                                                                   serverStatus,
                                                                   engineFactory,
                                                                   scheduler);

                // drools
                final DroolsEngine droolsEngine = new DroolsEngine(Collections.emptySet());
                final FilterService filterService = mock(FilterService.class);
                when(filterService.loadAll()).thenReturn(Collections.emptySet());

                filters.add(new ExtractorFilter(inputService));
                filters.add(new StaticFieldFilter(inputService));
                filters.add(new StreamMatcherFilter(streamRouter, mock(ThroughputStats.class)));
                if (!noDrools) {
                    filters.add(new RulesFilter(droolsEngine, filterService));
                }

                filterChain = new MessageFilterChainProcessor(metricRegistry,
                                                              filters,
                                                              mock(Journal.class),
                                                              serverStatus);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Benchmark
    public void testMessageFilterChain(final MessageFilterState state) {
        Message msg = new Message("original message", "test", Tools.nowUTC());
        state.filterChain.process(msg);
    }

    public static <T> T mock(Class<T> classToMock) {
        return Mockito.mock(classToMock, withSettings().stubOnly());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PipelineBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
