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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.LineProcessor;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import au.com.bytecode.opencsv.CSVParser;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.eaio.uuid.UUID;
import com.moandjiezana.toml.Toml;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
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
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
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
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.ScalarResult;
import org.openjdk.jmh.results.format.ResultFormatType;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import static com.google.common.collect.Iterables.getOnlyElement;

public class PipelinePerformanceBenchmarks {
    private static final Logger LOG = LoggerFactory.getLogger(PipelinePerformanceBenchmarks.class);
    public static final Message MESSAGE = new Message("hallo welt", "127.0.0.1", Tools.nowUTC());

    private static String benchmarkDir = System.getProperty("benchmarkDir", "benchmarks");

    private static MetricRegistry metricRegistry;

    @State(Scope.Benchmark)
    public static class PipelineConfig {

        // the parameter values are created dynamically
        @Param({})
        private String directoryName;

        @Param({"false", "true"})
        private String codeGenerator;

        @Param({"false", "true"})
        private String cachedStageIterators;

        private PipelineInterpreter interpreter;
        private BenchmarkConfig config;
        private Injector injector;
        private Iterator<Message> messageCycler;
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
                            bindConstant().annotatedWith(Names.named("processbuffer_processors")).to(1);
                            bindConstant().annotatedWith(Names.named("cached_stageiterators")).to(Boolean.valueOf(cachedStageIterators));
                            install(new FactoryModuleBuilder().build(PipelineInterpreter.State.Factory.class));
                        }
                    });

            // resolve types of benchmark configuration, to be loaded into the various services.
            Path path = Paths.get(benchmarkDir);
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
                        } else if (name.endsWith(".csv")) {
                            configFiles.put(Type.MESSAGES, inputFile);
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
            final PipelineStreamConnectionsService connectionsService = injector.getInstance(
                    PipelineStreamConnectionsService.class);
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

            final List<Message> loadedMessages = Lists.newArrayList();
            configFiles.get(Type.MESSAGES).forEach(file -> {
                try {
                    loadedMessages.addAll(com.google.common.io.Files.readLines(file,
                                                                               StandardCharsets.UTF_8,
                                                                               new CsvMessageFileProcessor()));
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.exit(-3);
                }
            });
            if (!loadedMessages.isEmpty()) {
                messageCycler = Iterators.cycle(loadedMessages);
            }

            if (!configFiles.containsKey(Type.MESSAGES)) {
                if ("generate".equalsIgnoreCase(config.messages)) {
                    final ArrayList<Message> objects = Lists.newArrayList();
                    Seq.range(0, 25000).forEach(i -> objects.add(new Message("hallo welt",
                                                                             "127.0.0.1",
                                                                             Tools.nowUTC())));
                    messageCycler = Iterators.cycle(objects);
                } else {
                    messageCycler = Iterators.cycle(MESSAGE);
                }
            }
            final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);
            // make the registry available to the profiler
            metricRegistry = metrics;

            // toggle code generation
            ConfigurationStateUpdater.setAllowCodeGeneration(Boolean.valueOf(codeGenerator));
            interpreter = injector.getInstance(PipelineInterpreter.class);
        }

        @TearDown
        public void dumpMetrics() throws Exception {

            // enable when using yourkit for single runs
//            if (controller != null) {
//                controller.captureSnapshot(Controller.SNAPSHOT_WITH_HEAP);
//            }
            final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);
            // make the registry available to the profiler
            metricRegistry = metrics;
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

        private static class CsvMessageFileProcessor implements LineProcessor<List<Message>> {
            String[] fieldNames;
            private CSVParser csvParser = new CSVParser();
            private List<Message> messages = Lists.newArrayList();
            boolean firstLine = true;

            @Override
            public boolean processLine(@Nonnull String line) throws IOException {
                final String[] strings = csvParser.parseLine(line);
                if (strings == null) {
                    return false;
                }
                if (firstLine) {
                    fieldNames = strings;
                    firstLine = false;
                    return true;
                }

                final Map<String, Object> fields = Seq.of(fieldNames)
                        .zipWithIndex()
                        .map(nameAndIndex -> nameAndIndex.map2(index -> strings[Math.toIntExact(index)]))
                        .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2));
                fields.put(Message.FIELD_ID, new UUID().toString());
                messages.add(new Message(fields));
                return true;
            }

            @Override
            public List<Message> getResult() {
                return messages;
            }
        }

        @SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection"})
        private class BenchmarkConfig {
            private String name;

            private List<StreamDescription> streams;

            private String messages;

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
            MESSAGES,
            PIPELINE
        }
    }

    @Benchmark
    public void runPipeline(PipelineConfig config, Blackhole bh) {
        // forever loop over the messages
        bh.consume(config.interpreter.process(config.messageCycler.next()));
    }

    public static void main(String[] args) throws RunnerException, URISyntaxException, IOException {

        final org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();
        new Option("b", "benchmarks", true, "Benchmark directory (default: 'benchmarks')").setRequired(false);
        options.addOption(Option.builder("b")
                                  .hasArg(true)
                                  .argName("directory")
                                  .longOpt("benchmarks")
                                  .desc("Benchmark directory (default: 'benchmarks')")
                                  .required(false)
                                  .build());
        options.addOption("f", true, "Number of forks (default 1). Set to 0 to allow attaching a debugger/profiler");
        options.addOption(Option.builder("n")
                                  .longOpt("name")
                                  .desc("Only run benchmark with the given name")
                                  .required(false)
                                  .argName("name")
                                  .hasArg(true)
                                  .build());
        options.addOption("h", "help");
        options.addOption("w", true, "Warmup iterations (default 5)");
        options.addOption("i", true, "Iterations (default 20)");

        String[] benchmarkParams = {};
        String benchmarkDir = "benchmarks";
        int forks = 1;
        int warmupIterations = 5;
        int iterations = 20;
        try {
            CommandLine line = new DefaultParser().parse(options, args);

            if (line.hasOption('h')) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("benchmark.sh", options);
                return;
            }

            benchmarkDir = line.getOptionValue('b', "benchmarks");
            benchmarkParams = loadBenchmarkNames(benchmarkDir).toArray(new String[]{});

            if (line.hasOption('n')) {
                benchmarkParams = new String[]{line.getOptionValue('n')};
            }

            if (line.hasOption('f')) {
                forks = Integer.parseInt(line.getOptionValue('f', "1"));
            }

            if (line.hasOption('w')) {
                warmupIterations = Integer.parseInt(line.getOptionValue('w', "5"));
            }
            if (line.hasOption('i')) {
                iterations = Integer.parseInt(line.getOptionValue('i', "20"));
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("benchmark.sh", options);
            System.exit(-2);
        }

        Options opt = new OptionsBuilder()
                .include(PipelinePerformanceBenchmarks.class.getSimpleName())
                .warmupIterations(warmupIterations)
                .warmupTime(TimeValue.seconds(5))
                .measurementIterations(iterations)
                .measurementTime(TimeValue.seconds(60))
                .detectJvmArgs()
                .threads(1)
                .forks(forks)
                .param("directoryName", benchmarkParams)
//                .param("codeGenerator", "false")
//                .param("cachedStageIterators", "false")
                .jvmArgsAppend("-DbenchmarkDir=" + benchmarkDir)
                .resultFormat(ResultFormatType.JSON)
                .addProfiler(GCProfiler.class)
                .addProfiler(MetricsProfiler.class)
                .build();

        final Runner runner = new Runner(opt);
        final Collection<RunResult> results = runner.run();
    }


    private static List<String> loadBenchmarkNames(String benchmarkDir) throws URISyntaxException, IOException {
        Path benchmarksPath = Paths.get(benchmarkDir);

        return Files.list(benchmarksPath)
                .map(Path::toFile)
                .filter(file -> !file.isHidden())
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    public static class MetricsProfiler implements InternalProfiler {

        @Override
        public String getDescription() {
            return "Metrics profile via MetricRegistry";
        }

        @Override
        public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {

        }

        @Override
        public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                           IterationParams iterationParams,
                                                           IterationResult result) {
            final ArrayList<Result> results = Lists.newArrayList();
            if (metricRegistry == null) {
                return results;
            }
            final SortedMap<String, Meter> counters = metricRegistry.getMeters((name, metric) -> {
                return name.startsWith(MetricRegistry.name(Rule.class)) || name.startsWith(MetricRegistry.name(Pipeline.class));
            });
            counters.entrySet()
                    .forEach(stringCounterEntry -> result.addResult(new ScalarResult(stringCounterEntry.getKey(),
                                                                                     stringCounterEntry.getValue().getCount(),
                                                                                     "calls",
                                                                                     AggregationPolicy.SUM)));

            return results;
        }
    }
}
