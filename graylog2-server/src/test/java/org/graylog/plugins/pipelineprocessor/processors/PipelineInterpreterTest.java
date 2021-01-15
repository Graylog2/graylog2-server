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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.codegen.CodeGenerator;
import org.graylog.plugins.pipelineprocessor.codegen.compiler.JavaCompiler;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryPipelineService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryRuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.journal.Journal;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineInterpreterTest {
    private static final RuleDao RULE_TRUE = RuleDao.create("true", "true", "true",
            "rule \"true\"\n" +
                    "when true\n" +
                    "then\n" +
                    "end", null, null);
    private static final RuleDao RULE_FALSE = RuleDao.create("false", "false", "false",
            "rule \"false\"\n" +
                    "when false\n" +
                    "then\n" +
                    "end", null, null);
    private static final RuleDao RULE_ADD_FOOBAR = RuleDao.create("add_foobar", "add_foobar", "add_foobar",
            "rule \"add_foobar\"\n" +
                    "when true\n" +
                    "then\n" +
                    "  set_field(\"foobar\", \"covfefe\");\n" +
                    "end", null, null);

    @Test
    public void testCreateMessage() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(Collections.singleton(
                RuleDao.create("abc",
                        "title",
                        "description",
                        "rule \"creates message\"\n" +
                                "when to_string($message.message) == \"original message\"\n" +
                                "then\n" +
                                "  create_message(\"derived message\");\n" +
                                "end",
                        Tools.nowUTC(),
                        null)
        ));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match all\n" +
                                "    rule \"creates message\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(
                CreateMessage.NAME, new CreateMessage(),
                StringConversion.NAME, new StringConversion());

        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        Message msg = messageInDefaultStream("original message", "test");
        final Messages processed = interpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        assertEquals(2, messages.length);
    }

    @Test
    public void testMatchAllContinuesIfAllRulesMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match all\n" +
                                "    rule \"true\";\n" +
                                "stage 1 match either\n" +
                                "    rule \"add_foobar\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(SetField.NAME, new SetField());
        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        final Messages processed = interpreter.process(messageInDefaultStream("message", "test"));

        final List<Message> messages = ImmutableList.copyOf(processed);
        assertThat(messages).hasSize(1);

        final Message actualMessage = messages.get(0);
        assertThat(actualMessage.getFieldAs(String.class, "foobar")).isEqualTo("covfefe");
    }

    @Test
    public void testMatchAllDoesNotContinueIfNotAllRulesMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match all\n" +
                                "    rule \"true\";\n" +
                                "    rule \"false\";\n" +
                                "stage 1 match either\n" +
                                "    rule \"add_foobar\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(SetField.NAME, new SetField());
        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        final Messages processed = interpreter.process(messageInDefaultStream("message", "test"));

        final List<Message> messages = ImmutableList.copyOf(processed);
        assertThat(messages).hasSize(1);

        final Message actualMessage = messages.get(0);
        assertThat(actualMessage.hasField("foobar")).isFalse();
    }

    @Test
    public void testMatchEitherContinuesIfOneRuleMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match either\n" +
                                "    rule \"true\";\n" +
                                "    rule \"false\";\n" +
                                "stage 1 match either\n" +
                                "    rule \"add_foobar\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(SetField.NAME, new SetField());
        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        final Messages processed = interpreter.process(messageInDefaultStream("message", "test"));

        final List<Message> messages = ImmutableList.copyOf(processed);
        assertThat(messages).hasSize(1);

        final Message actualMessage = messages.get(0);
        assertThat(actualMessage.getFieldAs(String.class, "foobar")).isEqualTo("covfefe");
    }

    @Test
    public void testMatchEitherStopsIfNoRuleMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match either\n" +
                                "    rule \"false\";\n" +
                                "stage 1 match either\n" +
                                "    rule \"add_foobar\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(SetField.NAME, new SetField());
        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        final Messages processed = interpreter.process(messageInDefaultStream("message", "test"));

        final List<Message> messages = ImmutableList.copyOf(processed);
        assertThat(messages).hasSize(1);

        final Message actualMessage = messages.get(0);
        assertThat(actualMessage.hasField("foobar")).isFalse();
    }

    @SuppressForbidden("Allow using default thread factory")
    private PipelineInterpreter createPipelineInterpreter(RuleService ruleService, PipelineService pipelineService, Map<String, Function<?>> functions) {
        final RuleMetricsConfigService ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        when(ruleMetricsConfigService.get()).thenReturn(RuleMetricsConfigDto.createDefault());
        final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(MongoDbPipelineStreamConnectionsService.class);
        final PipelineConnections pipelineConnections = PipelineConnections.create("p1", DEFAULT_STREAM_ID, Collections.singleton("p1"));
        when(pipelineStreamConnectionsService.loadAll()).thenReturn(Collections.singleton(pipelineConnections));

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry, new CodeGenerator(JavaCompiler::new));

        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                ruleMetricsConfigService,
                new MetricRegistry(),
                functionRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class),
                (currentPipelines, streamPipelineConnections, ruleMetricsConfig) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, ruleMetricsConfig, new MetricRegistry(), 1, true),
                false);
        return new PipelineInterpreter(
                mock(Journal.class),
                new MetricRegistry(),
                stateUpdater
        );
    }

    @Test
    @SuppressForbidden("Allow using default thread factory")
    public void testMetrics() {
        final RuleMetricsConfigService ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        when(ruleMetricsConfigService.get()).thenReturn(RuleMetricsConfigDto.createDefault());
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final RuleService ruleService = new InMemoryRuleService(clusterEventBus);
        ruleService.save(RuleDao.create("abc",
                "title",
                "description",
                "rule \"match_all\"\n" +
                        "when true\n" +
                        "then\n" +
                        "end",
                Tools.nowUTC(),
                null)
        );

        final PipelineService pipelineService = new InMemoryPipelineService(new ClusterEventBus());
        pipelineService.save(PipelineDao.create("cde", "title", "description",
                "pipeline \"pipeline\"\n" +
                        "stage 0 match all\n" +
                        "    rule \"match_all\";\n" +
                        "stage 1 match all\n" +
                        "    rule \"match_all\";\n" +
                        "end\n",
                Tools.nowUTC(),
                null)
        );

        final PipelineStreamConnectionsService pipelineStreamConnectionsService = new InMemoryPipelineStreamConnectionsService(clusterEventBus);
        pipelineStreamConnectionsService.save(PipelineConnections.create(null,
                DEFAULT_STREAM_ID,
                Collections.singleton("cde")));

        final FunctionRegistry functionRegistry = new FunctionRegistry(Collections.emptyMap());
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry, new CodeGenerator(JavaCompiler::new));

        final MetricRegistry metricRegistry = new MetricRegistry();
        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                ruleMetricsConfigService,
                metricRegistry,
                functionRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class),
                (currentPipelines, streamPipelineConnections, ruleMetricsConfig) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, ruleMetricsConfig, new MetricRegistry(), 1, true),
                false);
        final PipelineInterpreter interpreter = new PipelineInterpreter(
                mock(Journal.class),
                metricRegistry,
                stateUpdater
        );

        interpreter.process(messageInDefaultStream("", ""));

        final SortedMap<String, Meter> meters = metricRegistry.getMeters((name, metric) -> name.startsWith(name(Pipeline.class, "cde")) || name.startsWith(name(Rule.class, "abc")));

        assertThat(meters.keySet()).containsExactlyInAnyOrder(
                name(Pipeline.class, "cde", "executed"),
                name(Pipeline.class, "cde", "stage", "0", "executed"),
                name(Pipeline.class, "cde", "stage", "1", "executed"),
                name(Rule.class, "abc", "executed"),
                name(Rule.class, "abc", "cde", "0", "executed"),
                name(Rule.class, "abc", "cde", "1", "executed"),
                name(Rule.class, "abc", "matched"),
                name(Rule.class, "abc", "cde", "0", "matched"),
                name(Rule.class, "abc", "cde", "1", "matched"),
                name(Rule.class, "abc", "not-matched"),
                name(Rule.class, "abc", "cde", "0", "not-matched"),
                name(Rule.class, "abc", "cde", "1", "not-matched"),
                name(Rule.class, "abc", "failed"),
                name(Rule.class, "abc", "cde", "0", "failed"),
                name(Rule.class, "abc", "cde", "1", "failed")
        );

        assertThat(meters.get(name(Pipeline.class, "cde", "executed")).getCount()).isEqualTo(1L);
        assertThat(meters.get(name(Pipeline.class, "cde", "stage", "0", "executed")).getCount()).isEqualTo(1L);
        assertThat(meters.get(name(Pipeline.class, "cde", "stage", "1", "executed")).getCount()).isEqualTo(1L);

        assertThat(meters.get(name(Rule.class, "abc", "executed")).getCount()).isEqualTo(2L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "0", "executed")).getCount()).isEqualTo(1L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "1", "executed")).getCount()).isEqualTo(1L);

        assertThat(meters.get(name(Rule.class, "abc", "matched")).getCount()).isEqualTo(2L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "0", "matched")).getCount()).isEqualTo(1L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "1", "matched")).getCount()).isEqualTo(1L);

        assertThat(meters.get(name(Rule.class, "abc", "not-matched")).getCount()).isEqualTo(0L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "0", "not-matched")).getCount()).isEqualTo(0L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "1", "not-matched")).getCount()).isEqualTo(0L);

        assertThat(meters.get(name(Rule.class, "abc", "failed")).getCount()).isEqualTo(0L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "0", "failed")).getCount()).isEqualTo(0L);
        assertThat(meters.get(name(Rule.class, "abc", "cde", "1", "failed")).getCount()).isEqualTo(0L);

    }

    private Message messageInDefaultStream(String message, String source) {
        final Message msg = new Message(message, source, Tools.nowUTC());

        final Stream mockedStream = mock(Stream.class);
        when(mockedStream.getId()).thenReturn(DEFAULT_STREAM_ID);
        msg.addStream(mockedStream);

        return msg;
    }
}
