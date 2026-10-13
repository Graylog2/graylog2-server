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
package org.graylog.plugins.pipelineprocessor.compiler;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.listeners.InterpreterListener;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Runs rules through the real interpreter loop, interpreted and compiled, and compares everything observable. */
public final class DifferentialHarness {
    public static final boolean FIXTURES_ENABLED = Boolean.getBoolean("pipeline.compiler.differential");
    private static final long FIXED_NOW = new DateTime(2026, 1, 1, 12, 0, DateTimeZone.UTC).getMillis();

    private DifferentialHarness() {
    }

    public record Snapshot(Map<String, String> fields, boolean filteredOut, Set<String> streamIds, List<String> processingErrors) {
        public static Snapshot of(Message m, boolean created) {
            final Map<String, String> fields = new TreeMap<>();
            // message ids are random per message instance, so two runs on equal input never agree on them
            m.getFields().forEach((k, v) -> fields.put(k, "_id".equals(k) || (created && "gl2_message_id".equals(k)) ? "<generated>" : typed(v)));
            final List<String> errors = m.processingErrors().stream()
                    .map(e -> e.getCause() + " | " + e.getMessage() + " | " + e.getDetails()).toList();
            final Set<String> streamIds = new TreeSet<>(Comparator.nullsFirst(Comparator.<String>naturalOrder()));
            streamIds.addAll(m.getStreamIds());
            return new Snapshot(fields, m.getFilterOut(), streamIds, errors);
        }
    }

    public record Outcome(Snapshot message, List<Snapshot> created, List<String> events) {}

    /** Value plus its Java type, recursively, with maps in key order. */
    static String typed(@Nullable Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map<?, ?> map) {
            final Map<String, String> out = new TreeMap<>();
            map.forEach((k, val) -> out.put(String.valueOf(k), typed(val)));
            return v.getClass().getName() + out;
        }
        if (v instanceof Iterable<?> iterable) {
            final List<String> out = new ArrayList<>();
            iterable.forEach(e -> out.add(typed(e)));
            return v.getClass().getName() + out;
        }
        if (v.getClass().isArray()) {
            final List<String> out = new ArrayList<>();
            for (int i = 0; i < Array.getLength(v); i++) {
                out.add(typed(Array.get(v, i)));
            }
            return v.getClass().getName() + out;
        }
        return v.getClass().getName() + ":" + v;
    }

    public static Outcome run(List<Rule> rules, Message message,
                              @Nullable java.util.function.Function<Rule, CompiledRule> compiler) {
        final List<Rule> stageRules = rules.stream().map(Rule::copy).toList();
        if (compiler != null) {
            stageRules.forEach(r -> r.setCompiled(compiler.apply(r)));
        }
        final Stage stage = Stage.builder().stage(0).match(Stage.Match.EITHER)
                .ruleReferences(stageRules.stream().map(Rule::name).toList()).build();
        stage.setRules(stageRules);
        final Pipeline pipeline = Pipeline.builder().id("differential").name("differential")
                .stages(new TreeSet<>(Set.of(stage))).build();
        stage.setPipeline(pipeline);
        // the interpreter marks these meters for every rule that has an id
        final PipelineMetricRegistry pipelineMetrics = PipelineMetricRegistry.create(new MetricRegistry(), "pipelines", "rules");
        stageRules.forEach(r -> r.registerMetrics(pipelineMetrics, pipeline.id(), stage.stage()));
        stage.registerMetrics(pipelineMetrics, pipeline.id());
        pipeline.registerMetrics(pipelineMetrics);
        final PipelineInterpreter.State state = new PipelineInterpreter.State(ImmutableMap.of("differential", pipeline),
                ImmutableSetMultimap.of(), RuleMetricsConfigDto.createDefault(), new MetricRegistry(), 1, true);
        final PipelineInterpreter interpreter = new PipelineInterpreter(mock(MessageQueueAcknowledger.class), new MetricRegistry(), null, 1);
        final RecordingListener listener = new RecordingListener();
        return withFixedClock(() -> {
            final List<Message> created = interpreter.processForResolvedPipelines(message, message.getId(), Set.of(pipeline), listener, state);
            return new Outcome(Snapshot.of(message, false), created.stream().map(m -> Snapshot.of(m, true)).toList(), listener.events);
        });
    }

    public static void assertEquivalent(Supplier<Message> input, List<Rule> rules,
                                        java.util.function.Function<Rule, CompiledRule> compiler) {
        final Outcome interpreted = run(rules, input.get(), null);
        final Outcome compiled = run(rules, input.get(), compiler);
        assertThat(compiled)
                .as("DIFFERENTIAL MISMATCH for rules %s", rules.stream().map(Rule::name).toList())
                .isEqualTo(interpreted);
    }

    public static <T> T withFixedClock(Supplier<T> body) {
        // restore whatever clock the caller had, fixture tests may have fixed it themselves
        final DateTimeUtils.MillisProvider previous = currentMillisProvider();
        DateTimeUtils.setCurrentMillisFixed(FIXED_NOW);
        try {
            return body.get();
        } finally {
            DateTimeUtils.setCurrentMillisProvider(previous);
        }
    }

    /** Joda has no public getter for its clock, and fixture tests may have fixed it themselves. */
    @SuppressForbidden("Joda exposes no getter for its clock")
    private static DateTimeUtils.MillisProvider currentMillisProvider() {
        try {
            final Field field = DateTimeUtils.class.getDeclaredField("cMillisProvider");
            field.setAccessible(true);
            return (DateTimeUtils.MillisProvider) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read Joda's current clock", e);
        }
    }

    /** Copies a message, including nested mutable collections, so two runs can't see each other's in-place changes. */
    public static Message copyOf(Message m, MessageFactory factory) {
        final Map<String, Object> fields = new HashMap<>();
        m.getFields().forEach((k, v) -> fields.put(k, copyValue(v)));
        final Message copy = factory.createMessage(fields);
        copy.addStreams(m.getStreams());
        return copy;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object copyValue(Object v) {
        if (v instanceof LinkedHashMap<?, ?> map) {
            final LinkedHashMap copy = new LinkedHashMap();
            map.forEach((k, val) -> copy.put(k, copyValue(val)));
            return copy;
        }
        if (v instanceof HashMap<?, ?> map) {
            final HashMap copy = new HashMap();
            map.forEach((k, val) -> copy.put(k, copyValue(val)));
            return copy;
        }
        if (v instanceof ArrayList<?> list) {
            final ArrayList copy = new ArrayList();
            list.forEach(e -> copy.add(copyValue(e)));
            return copy;
        }
        return v; // immutable or unknown: keep as is
    }

    /** Verifies generated bytecode with ASM's CheckClassAdapter. Implemented in Task 8 once asm-util is available. */
    public static void verify(byte[] classBytes) {
    }

    /** Records every listener callback in order. */
    public static final class RecordingListener implements InterpreterListener {
        public final List<String> events = new ArrayList<>();

        @Override
        public void startProcessing() {
            events.add("startProcessing");
        }

        @Override
        public void finishProcessing() {
            events.add("finishProcessing");
        }

        @Override
        public void processStreams(Message message, Set<Pipeline> pipelines, Set<String> streams) {
            events.add("processStreams " + new TreeSet<>(streams));
        }

        @Override
        public void enterStage(Stage stage) {
            events.add("enterStage " + stage.stage());
        }

        @Override
        public void exitStage(Stage stage) {
            events.add("exitStage " + stage.stage());
        }

        @Override
        public void evaluateRule(Rule rule, Pipeline pipeline) {
            events.add("evaluateRule " + rule.name());
        }

        @Override
        public void failEvaluateRule(Rule rule, Pipeline pipeline) {
            events.add("failEvaluateRule " + rule.name());
        }

        @Override
        public void satisfyRule(Rule rule, Pipeline pipeline) {
            events.add("satisfyRule " + rule.name());
        }

        @Override
        public void dissatisfyRule(Rule rule, Pipeline pipeline) {
            events.add("dissatisfyRule " + rule.name());
        }

        @Override
        public void executeRule(Rule rule, Pipeline pipeline) {
            events.add("executeRule " + rule.name());
        }

        @Override
        public void finishExecuteRule(Rule rule, Pipeline pipeline) {
            events.add("finishExecuteRule " + rule.name());
        }

        @Override
        public void failExecuteRule(Rule rule, Pipeline pipeline) {
            events.add("failExecuteRule " + rule.name());
        }

        @Override
        public void continuePipelineExecution(Pipeline pipeline, Stage stage) {
            events.add("continue " + stage.stage());
        }

        @Override
        public void stopPipelineExecution(Pipeline pipeline, Stage stage) {
            events.add("stop " + stage.stage());
        }
    }
}
