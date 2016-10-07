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

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.moandjiezana.toml.Toml;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryServicesModule;
import org.graylog.plugins.pipelineprocessor.functions.ProcessorFunctionsModule;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.InMemoryGrokPatternService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.bindings.providers.MetricRegistryProvider;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.NoopJournal;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.getOnlyElement;

public class PipelinePerformanceBenchmarks {
    private static final Logger LOG = LoggerFactory.getLogger(PipelinePerformanceBenchmarks.class);

    private static final String BENCHMARKS_RESOURCE_DIRECTORY = "/benchmarks";
    private static final Message MESSAGE = new Message("hallo welt", "127.0.0.1", Tools.nowUTC());

    @State(Scope.Benchmark)
    public static class PipelineConfig {

        // the parameter values are created dynamically
        @Param({})
        private String directoryName;
        private PipelineInterpreter interpreter;
        private BenchmarkConfig config;
        private Injector injector;
        // enable when using yourkit for single runs
//        private Controller controller;

        @Setup
        public void setup() throws Exception {

            // enable when using yourkit for single runs
//            controller = new Controller();
//            controller.startCPUTracing(null);
//            controller.startAllocationRecording(null);
//            controller.enableStackTelemetry();

            injector = Guice.createInjector(
                    new ProcessorFunctionsModule(),
                    new SchedulerBindings(),
                    new InMemoryServicesModule(),
                    new AbstractModule() {
                        @Override
                        protected void configure() {
                            bind(Journal.class).to(NoopJournal.class).asEagerSingleton();
                            bind(StreamService.class).toInstance(new DummyStreamService());
                            bind(GrokPatternService.class).to(InMemoryGrokPatternService.class);
                            bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class);
                        }
                    });

            // resolve types of benchmark configuration, to be loaded into the various services.
            final Path path = getResourcePath();
            Multimap<Type, File> configFiles = MultimapBuilder
                    .enumKeys(Type.class)
                    .arrayListValues()
                    .build();
            Files.list(path.resolve(directoryName))
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .forEach(inputFile -> {
                        final String name = inputFile.getName();
                        if (name.endsWith(".rule")) {
                            configFiles.put(Type.RULE, inputFile);
                        } else if (name.endsWith(".pipeline")) {
                            configFiles.put(Type.PIPELINE, inputFile);
                        } else if (name.equals("benchmark.toml")) {
                            configFiles.put(Type.CONFIG, inputFile);
                        } else {
                            LOG.warn("unrecognized file {} found, it will be ignored.", inputFile);
                        }
                    });

            if (configFiles.containsKey(Type.CONFIG)) {
                config = new Toml().read(getOnlyElement(configFiles.get(Type.CONFIG))).to(BenchmarkConfig.class);
            } else {
                LOG.error("The benchmark directory must include a benchmark.toml file! Aborting.");
                System.exit(-1);
            }
            final PipelineRuleParser parser = injector.getInstance(PipelineRuleParser.class);
            final RuleService ruleService = injector.getInstance(RuleService.class);

            configFiles.get(Type.RULE).forEach(file -> {
                final String ruleText = readFile(file);
                if (ruleText == null) {
                    return;
                }
                final Rule rule = parser.parseRule(ruleText, true);
                final DateTime now = Tools.nowUTC();
                final RuleDao saved = ruleService.save(RuleDao.create(null,
                                                                      rule.name(),
                                                                      null,
                                                                      ruleText,
                                                                      now,
                                                                      now));
                LOG.debug("Read and saved rule {} with Id {}", saved.title(), saved.id());
            });

            final PipelineService pipelineService = injector.getInstance(PipelineService.class);
            configFiles.get(Type.PIPELINE).forEach(file -> {
                final String pipelineText = readFile(file);
                if (pipelineText == null) {
                    return;
                }
                final Pipeline pipeline = parser.parsePipeline(null, pipelineText);
                final DateTime now = Tools.nowUTC();
                final PipelineDao saved = pipelineService.save(PipelineDao.create(null,
                                                                                  pipeline.name(),
                                                                                  null,
                                                                                  pipelineText,
                                                                                  now,
                                                                                  now));
                LOG.debug("Read and saved pipeline {} with Id {}", saved.title(), saved.id());
            });
            final ImmutableMap<String, PipelineDao> pipelineTitleIndex = Maps.uniqueIndex(pipelineService.loadAll(),
                                                                                                     PipelineDao::title);
            final PipelineStreamConnectionsService connectionsService = injector.getInstance(PipelineStreamConnectionsService.class);
            final StreamService streamService = injector.getInstance(StreamService.class);
            if (config.streams == null || config.streams.isEmpty()) {
                LOG.info("No streams defined, this benchmark won't match any messages!");
            } else {
                for (BenchmarkConfig.StreamDescription streamDescription : config.streams) {
                    final Stream stream = streamService.create(Collections.emptyMap());
                    stream.setTitle(streamDescription.name);
                    if (streamDescription.name.equals("default")) {
                        stream.setDefaultStream(true);
                    }
                    stream.setDescription(streamDescription.description);
                    stream.setDisabled(false);
                    final String id = streamService.save(stream);

                    // TODO default stream handling is really wonky now.
                    connectionsService.save(PipelineConnections.create(null, stream.isDefaultStream() ? "default" : id,
                                                                       streamDescription.pipelines.stream()
                                                                               .map(pipelineTitleIndex::get)
                                                                               .map(PipelineDao::id)
                                                                               .collect(Collectors.toSet())));
                }
            }

            interpreter = injector.getInstance(PipelineInterpreter.class);
        }

        @TearDown
        public void dumpMetrics() throws Exception {

            // enable when using yourkit for single runs
//            controller.captureSnapshot(Controller.SNAPSHOT_WITH_HEAP);
            final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

            final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                    .outputTo(new PrintStream("/tmp/bench-" + directoryName + ".txt"))
                    .build();
            reporter.report();

        }

        private String readFile(File file) {
            try {
                return com.google.common.io.Files.toString(file, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOG.error("Cannot read rule file, skipping it. This will likely fail the benchmark.", e);
                return null;
            }
        }

        /**
         * Dummy stream service that only allows setting and getting stream definitions, but no rules, alert conditions, receivers or outputs.
         */
        private static class DummyStreamService implements StreamService {

            private final Map<String, Stream> store = new MapMaker().makeMap();

            @Override
            public Stream create(Map<String, Object> fields) {
                return new StreamImpl(fields);
            }

            @Override
            public Stream create(CreateStreamRequest cr, String userId) {
                Map<String, Object> streamData = Maps.newHashMap();
                streamData.put(StreamImpl.FIELD_TITLE, cr.title());
                streamData.put(StreamImpl.FIELD_DESCRIPTION, cr.description());
                streamData.put(StreamImpl.FIELD_CREATOR_USER_ID, userId);
                streamData.put(StreamImpl.FIELD_CREATED_AT, Tools.nowUTC());
                streamData.put(StreamImpl.FIELD_CONTENT_PACK, cr.contentPack());
                streamData.put(StreamImpl.FIELD_MATCHING_TYPE, cr.matchingType().toString());

                return create(streamData);
            }

            @Override
            public Stream load(String id) throws NotFoundException {
                final Stream stream = store.get(id);
                if (stream == null) {
                    throw new NotFoundException();
                }
                return stream;
            }

            @Override
            public void destroy(Stream stream) throws NotFoundException {
                if (store.remove(stream.getId()) == null) {
                    throw new NotFoundException();
                }
            }

            @Override
            public List<Stream> loadAll() {
                return ImmutableList.copyOf(store.values());
            }

            @Override
            public List<Stream> loadAllEnabled() {
                return store.values().stream().filter(stream -> !stream.getDisabled()).collect(Collectors.toList());
            }

            @Override
            public long count() {
                return store.size();
            }

            @Override
            public void pause(Stream stream) throws ValidationException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void resume(Stream stream) throws ValidationException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public List<StreamRule> getStreamRules(Stream stream) throws NotFoundException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public List<Stream> loadAllWithConfiguredAlertConditions() {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public List<AlertCondition> getAlertConditions(Stream stream) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public AlertCondition getAlertCondition(Stream stream,
                                                    String conditionId) throws NotFoundException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void addAlertCondition(Stream stream,
                                          AlertCondition condition) throws ValidationException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void updateAlertCondition(Stream stream,
                                             AlertCondition condition) throws ValidationException {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void removeAlertCondition(Stream stream, String conditionId) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void addAlertReceiver(Stream stream, String type, String name) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void removeAlertReceiver(Stream stream, String type, String name) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void addOutput(Stream stream, Output output) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void removeOutput(Stream stream, Output output) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public void removeOutputFromAllStreams(Output output) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public <T extends Persisted> int destroy(T model) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public <T extends Persisted> int destroyAll(Class<T> modelClass) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public <T extends Persisted> String save(T model) throws ValidationException {
                store.put(model.getId(), (Stream) model);
                return model.getId();
            }

            @Override
            public <T extends Persisted> String saveWithoutValidation(T model) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model,
                                                                                      Map<String, Object> fields) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
                throw new IllegalStateException("no implemented");
            }

            @Override
            public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators,
                                                                Map<String, Object> fields) {
                throw new IllegalStateException("no implemented");
            }
        }

        @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
        private class BenchmarkConfig {
            private String name;

            private List<StreamDescription> streams;

            private class StreamDescription {
                private String name;
                private String description;
                private Set<String> pipelines;
            }
        }

        /**
         * type of configuration file, either global config (name, streams, connections), a rule source file, a pipeline source file
         */
        private enum Type {
            CONFIG,
            RULE,
            PIPELINE
        }
    }

    @Benchmark
    public void runPipeline(PipelineConfig config, Blackhole bh) {
        bh.consume(config.interpreter.process(MESSAGE));
    }

    public static void main(String[] args) throws RunnerException, URISyntaxException, IOException {
        boolean fork = System.getProperty("profile") == null;

        final String singleBenchmarkName = System.getProperty("benchmark.name");
        final String[] values = singleBenchmarkName != null ?
                new String[] {singleBenchmarkName} :
                loadBenchmarkNames().toArray(new String[]{});

        Options opt = new OptionsBuilder()
                .include(PipelinePerformanceBenchmarks.class.getSimpleName())
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(20))
                .threads(1)
                .forks(fork ? 1 : 0)
                .param("directoryName", values)
                .build();

        new Runner(opt).run();
    }


    private static List<String> loadBenchmarkNames() throws URISyntaxException, IOException {
        Path benchmarksPath = getResourcePath();

        return Files.list(benchmarksPath)
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    private static Path getResourcePath() throws URISyntaxException, IOException {
        final URI benchmarks = PipelinePerformanceBenchmarks.class.getResource(BENCHMARKS_RESOURCE_DIRECTORY).toURI();

        Path benchmarksPath;
        if (benchmarks.getScheme().equals("jar")) {
            FileSystem fileSystem = FileSystems.newFileSystem(benchmarks, Collections.emptyMap());
            benchmarksPath = fileSystem.getPath(BENCHMARKS_RESOURCE_DIRECTORY);
            fileSystem.close();
        } else {
            benchmarksPath = Paths.get(benchmarks);
        }
        return benchmarksPath;
    }
}
