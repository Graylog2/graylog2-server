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
package org.graylog.benchmarks.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Provider;

import com.codahale.metrics.MetricRegistry;

import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.codegen.CodeGenerator;
import org.graylog.plugins.pipelineprocessor.codegen.compiler.JavaCompiler;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
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
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rules.DroolsEngine;
import org.graylog2.shared.journal.Journal;
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
public class FilterchainVsPipeline {

    @State(Scope.Benchmark)
    public static class InterpreterState {

        private final PipelineInterpreter interpreter;

        public InterpreterState() {
            final RuleService ruleService = mock(MongoDbRuleService.class);
            when(ruleService.loadAll()).thenReturn(Collections.singleton(
                    RuleDao.create("abc",
                                   "title",
                                   "description",
                                   "rule \"add\"\n" +
                                           "when tostring($message.message) == \"original message\"\n" +
                                           "then\n" +
                                           "  set_field(\"field\", \"derived message\");\n" +
                                           "end",
                                   Tools.nowUTC(),
                                   null)
            ));

            final PipelineService pipelineService = mock(MongoDbPipelineService.class);
            when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                    PipelineDao.create("cde", "title", "description",
                                       "pipeline \"pipeline\"\n" +
                                               "stage 0 match all\n" +
                                               "    rule \"add\";\n" +
                                               "end\n",
                                       Tools.nowUTC(),
                                       null)
            ));

            final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(MongoDbPipelineStreamConnectionsService.class);
            final PipelineConnections pipelineStreamConnection = PipelineConnections.create(null,
                                                                                                      "default",
                                                                                                      newHashSet("cde"));
            when(pipelineStreamConnectionsService.loadAll()).thenReturn(
                    newHashSet(pipelineStreamConnection)
            );

            final Map<String, Function<?>> functions = Maps.newHashMap();
            functions.put(SetField.NAME, new SetField());
            functions.put(StringConversion.NAME, new StringConversion());

            final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
            final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry, new CodeGenerator(JavaCompiler::new));

            final MetricRegistry metricRegistry = new MetricRegistry();
            final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                    pipelineService,
                    pipelineStreamConnectionsService,
                    parser,
                    new MetricRegistry(),
                    functionRegistry,
                    Executors.newScheduledThreadPool(1),
                    mock(EventBus.class),
                    (currentPipelines, streamPipelineConnections, classLoader) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, null, metricRegistry, 1, true),
                    false);
            interpreter = new PipelineInterpreter(
                    mock(Journal.class),
                    metricRegistry,
                    mock(EventBus.class),
                    stateUpdater
            );
        }

    }

    @Benchmark
    public Messages testPipeline(final InterpreterState state) {
        Message msg = new Message("original message", "test", Tools.nowUTC());
        return state.interpreter.process(msg);
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

                final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setDaemon(
                        true).build());

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
                final EventBus eventBus = new EventBus();
                ExecutorService daemonExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(
                        true).build());
                when(engineFactory.create(any(), any())).thenReturn(
                        new StreamRouterEngine(Collections.emptyList(),
                                daemonExecutor,
                                streamFaultManager,
                                streamMetrics,
                                (Provider<Stream>) () -> null)
                );
                final StreamRouter streamRouter = new StreamRouter(streamService,
                                                                   serverStatus,
                                                                   engineFactory,
                                                                   eventBus,
                                                                   scheduler);

                // drools
                final DroolsEngine droolsEngine = new DroolsEngine(Collections.emptySet());
                final FilterService filterService = mock(FilterService.class);
                when(filterService.loadAll()).thenReturn(Collections.emptySet());

                filters.add(new ExtractorFilter(inputService, eventBus, scheduler));
                filters.add(new StaticFieldFilter(inputService, eventBus, scheduler));
                filters.add(new StreamMatcherFilter(streamRouter));
                if (!noDrools) {
                    filters.add(new RulesFilter(droolsEngine, filterService, eventBus, scheduler));
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
    public Messages testMessageFilterChain(final MessageFilterState state) {
        Message msg = new Message("original message", "test", Tools.nowUTC());
        return state.filterChain.process(msg);
    }

    public static <T> T mock(Class<T> classToMock) {
        return Mockito.mock(classToMock, withSettings().stubOnly());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(FilterchainVsPipeline.class.getSimpleName())
                .warmupIterations(5)
                .measurementIterations(5)
                .threads(1)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
