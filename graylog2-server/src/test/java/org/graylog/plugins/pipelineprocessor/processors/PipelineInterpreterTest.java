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
import org.graylog.failure.ProcessingFailureCause;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
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
import org.graylog.plugins.pipelineprocessor.functions.conversion.DoubleConversion;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.DropMessage;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageCollection;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineInterpreterTest {
    private static final RuleDao RULE_TRUE = RuleDao.create("true", null, "true", "true",
            """
                    rule "true"
                    when true
                    then
                    end""", null, null, null, null);
    private static final RuleDao RULE_FALSE = RuleDao.create("false", null, "false", "false",
            """
                    rule "false"
                    when false
                    then
                    end""", null, null, null, null);
    private static final RuleDao RULE_ADD_FOOBAR = RuleDao.create("add_foobar", null, "add_foobar", "add_foobar",
            """
                    rule "add_foobar"
                    when true
                    then
                      set_field("foobar", "covfefe");
                    end""", null, null, null, null);
    private static final java.util.function.Function<String, RuleDao> RULE_SET_FIELD =
            (name) -> RuleDao.create("false", null, "false", "false", "rule \"" + name + "\"\n" +
                                                                      "when true\n" +
                                                                      "then\n" +
                                                                      "  set_field(\"" + name + "\", \"value\");" +
                                                                      "end", null, null, null, null);
    private static final RuleDao RULE_DROP_MESSAGE = RuleDao.create("false", null, "false", "false",
            """
                    rule "drop_message"
                    when true
                    then
                      drop_message();\
                    end""", null, null, null, null);
    private final MessageQueueAcknowledger messageQueueAcknowledger = mock(MessageQueueAcknowledger.class);

    private final RuleService ruleService = Mockito.mock(RuleService.class);
    private final PipelineService pipelineService = Mockito.mock(PipelineService.class);
    private final MessageFactory messageFactory = new TestMessageFactory();

    @Test
    public void testCreateMessage() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(Collections.singleton(
                RuleDao.create("abc", null, "title", "description",
                        """
                                rule "creates message"
                                when to_string($message.message) == "original message"
                                then
                                  create_message("derived message");
                                end""", Tools.nowUTC(), null, null, null)
        ));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "creates message";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(
                CreateMessage.NAME, new CreateMessage(messageFactory),
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
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "true";
                                stage 1 match either
                                    rule "add_foobar";
                                end
                                """,
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
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "true";
                                    rule "false";
                                stage 1 match either
                                    rule "add_foobar";
                                end
                                """,
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
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match either
                                    rule "true";
                                    rule "false";
                                stage 1 match either
                                    rule "add_foobar";
                                end
                                """,
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
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match either
                                    rule "false";
                                stage 1 match either
                                    rule "add_foobar";
                                end
                                """,
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
    public void testMatchPassContinuesIfOneRuleMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match pass
                                    rule "true";
                                    rule "false";
                                stage 1 match pass
                                    rule "add_foobar";
                                end
                                """,
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
    public void testMatchPassContinuesIfNoRuleMatched() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RULE_TRUE, RULE_FALSE, RULE_ADD_FOOBAR));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match pass
                                    rule "false";
                                stage 1 match pass
                                    rule "add_foobar";
                                end
                                """,
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
    public void testDroppedMessageWillHaltProcessingAfterCurrentStage() {
        final RuleService ruleService = mock(MongoDbRuleService.class);
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(
                RULE_SET_FIELD.apply("1-a"),
                RULE_SET_FIELD.apply("1-b"),
                RULE_SET_FIELD.apply("2-a"),
                RULE_SET_FIELD.apply("2-b"),
                RULE_DROP_MESSAGE
        ));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(ImmutableList.of(
                PipelineDao.create("p1", null, "title1", "description",
                        """
                                pipeline "pipeline1"
                                stage 0 match pass
                                    rule "1-a";
                                    rule "drop_message";
                                stage 1 match pass
                                    rule "1-b";
                                end
                                """,
                        Tools.nowUTC(),
                        null),
                PipelineDao.create("p2", null, "title2", "description",
                        """
                                pipeline "pipeline2"
                                stage 0 match pass
                                    rule "2-a";
                                stage 1 match pass
                                    rule "2-b";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));

        final Map<String, Function<?>> functions = ImmutableMap.of(
                SetField.NAME, new SetField(),
                DropMessage.NAME, new DropMessage()
        );
        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, functions);

        final Messages processed = interpreter.process(messageInDefaultStream("message", "test"));

        assertThat(processed).isInstanceOf(MessageCollection.class);

        // Use MessageCollection#source here to get access to the unfiltered messages
        final List<Message> messages = ImmutableList.copyOf(((MessageCollection) processed).source());
        assertThat(messages).hasSize(1);

        final Message actualMessage = messages.get(0);

        assertThat(actualMessage.getFilterOut()).isTrue();

        // Even though "drop_message" has been called in one of the stages, all stages of the same number should
        // have been executed
        assertThat(actualMessage.getFieldAs(String.class, "1-a")).isEqualTo("value");
        assertThat(actualMessage.getFieldAs(String.class, "2-a")).isEqualTo("value");

        // The second stage in both pipelines should not have been executed due to the "drop_message" call
        assertThat(actualMessage.getField("1-b")).isNull();
        assertThat(actualMessage.getField("2-b")).isNull();
    }

    @SuppressForbidden("Allow using default thread factory")
    private PipelineInterpreter createPipelineInterpreter(RuleService ruleService, PipelineService pipelineService, Map<String, Function<?>> functions) {
        final RuleMetricsConfigService ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        when(ruleMetricsConfigService.get()).thenReturn(RuleMetricsConfigDto.createDefault());
        final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(MongoDbPipelineStreamConnectionsService.class);
        final Set<String> pipelineIds = pipelineService.loadAll().stream().map(PipelineDao::id).collect(Collectors.toSet());
        final PipelineConnections pipelineConnections = PipelineConnections.create("p1", DEFAULT_STREAM_ID, pipelineIds);
        when(pipelineStreamConnectionsService.loadAll()).thenReturn(Collections.singleton(pipelineConnections));

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);

        final MetricRegistry metricRegistry = new MetricRegistry();
        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                (config, ruleParser) -> new PipelineResolver(ruleParser, config),
                ruleMetricsConfigService,
                metricRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class),
                (currentPipelines, streamPipelineConnections, ruleMetricsConfig) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, ruleMetricsConfig, metricRegistry, 1, true)
        );
        return new PipelineInterpreter(
                messageQueueAcknowledger,
                metricRegistry,
                stateUpdater);
    }

    @Test
    @SuppressForbidden("Allow using default thread factory")
    public void testMetrics() {
        final RuleMetricsConfigService ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        when(ruleMetricsConfigService.get()).thenReturn(RuleMetricsConfigDto.createDefault());
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        final RuleService ruleService = new InMemoryRuleService(clusterEventBus);
        DateTime createdAt = Tools.nowUTC();
        ruleService.save(RuleDao.create("abc", null, "title", "description",
                """
                        rule "match_all"
                        when true
                        then
                        end""", createdAt, null, null, null)
        );

        final PipelineService pipelineService = new InMemoryPipelineService(new ClusterEventBus());
        pipelineService.save(PipelineDao.create("cde", null, "title", "description",
                """
                        pipeline "pipeline"
                        stage 0 match all
                            rule "match_all";
                        stage 1 match all
                            rule "match_all";
                        end
                        """,
                Tools.nowUTC(),
                null)
        );

        final PipelineStreamConnectionsService pipelineStreamConnectionsService = new InMemoryPipelineStreamConnectionsService(clusterEventBus);
        pipelineStreamConnectionsService.save(PipelineConnections.create(null,
                DEFAULT_STREAM_ID,
                Collections.singleton("cde")));

        final FunctionRegistry functionRegistry = new FunctionRegistry(Collections.emptyMap());
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);

        final MetricRegistry metricRegistry = new MetricRegistry();
        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                (config, ruleParser) -> new PipelineResolver(ruleParser, config),
                ruleMetricsConfigService,
                metricRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class),
                (currentPipelines, streamPipelineConnections, ruleMetricsConfig) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, ruleMetricsConfig, new MetricRegistry(), 1, true)
        );
        final PipelineInterpreter interpreter = new PipelineInterpreter(
                mock(MessageQueueAcknowledger.class),
                metricRegistry,
                stateUpdater);

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

    @Test
    public void process_ruleConditionEvaluationErrorConvertedIntoMessageProcessingError() throws Exception {
        // given
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RuleDao.create("broken_condition", null, "broken_condition", "broken_condition",
                """
                        rule "broken_condition"
                        when
                            to_double($message.num * $message.num) > 0.0
                        then
                            set_field("num_sqr", $message.num * $message.num);
                        end""", null, null, null, null)));

        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "broken_condition";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));


        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, ImmutableMap.of(
                SetField.NAME, new SetField(),
                DoubleConversion.NAME, new DoubleConversion())
        );

        // when
        final List<Message> processed = extractMessagesFromMessageCollection(interpreter.process(messageWithNumField("ABC")));

        // then
        assertThat(processed)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(m -> {
                    assertThat(m.processingErrors())
                            .hasSize(1)
                            .hasOnlyOneElementSatisfying(pe -> {
                                assertThat(pe.getCause()).isEqualTo(ProcessingFailureCause.RuleConditionEvaluationError);
                                assertThat(pe.getMessage()).isEqualTo("Error evaluating condition for rule <broken_condition/broken_condition> (pipeline <pipeline/p1>)");
                                assertThat(pe.getDetails()).isEqualTo("In call to function 'to_double' at 3:4 an exception was thrown: class java.lang.String cannot be cast to class java.lang.Double (java.lang.String and java.lang.Double are in module java.base of loader 'bootstrap')");
                            });
                });
    }

    @Test
    public void process_ruleStatementEvaluationErrorConvertedIntoMessageProcessingError() throws Exception {
        // given
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RuleDao.create("broken_statement", null, "broken_statement", "broken_statement",
                """
                        rule "broken_statement"
                        when
                            has_field("num")
                        then
                            set_field("num_sqr", $message.num * $message.num);
                        end""", null, null, null, null)));

        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "broken_statement";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));


        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, ImmutableMap.of(
                SetField.NAME, new SetField(),
                DoubleConversion.NAME, new DoubleConversion(),
                HasField.NAME, new HasField()
        ));

        // when
        final List<Message> processed = extractMessagesFromMessageCollection(interpreter.process(messageWithNumField(Long.valueOf(1))));

        // then
        assertThat(processed)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(m -> {
                    assertThat(m.processingErrors())
                            .hasSize(1)
                            .hasOnlyOneElementSatisfying(pe -> {
                                assertThat(pe.getCause()).isEqualTo(ProcessingFailureCause.RuleStatementEvaluationError);
                                assertThat(pe.getMessage()).isEqualTo("Error evaluating action for rule <broken_statement/broken_statement> (pipeline <pipeline/p1>)");
                                assertThat(pe.getDetails()).isEqualTo("In call to function 'set_field' at 5:4 an exception was thrown: class java.lang.Long cannot be cast to class java.lang.Double (java.lang.Long and java.lang.Double are in module java.base of loader 'bootstrap')");
                            });
                });
    }

    @Test
    public void process_noEvaluationErrorsResultIntoNoMessageProcessingErrors() {
        // given
        when(ruleService.loadAll()).thenReturn(ImmutableList.of(RuleDao.create("valid_rule", null, "valid_rule", "valid_rule",
                """
                        rule "valid_rule"
                        when
                            has_field("num")
                        then
                            set_field("num_sqr", to_double($message.num) * to_double($message.num));
                        end""", null, null, null, null)));

        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", null, "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "valid_rule";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));


        final PipelineInterpreter interpreter = createPipelineInterpreter(ruleService, pipelineService, ImmutableMap.of(
                SetField.NAME, new SetField(),
                DoubleConversion.NAME, new DoubleConversion(),
                HasField.NAME, new HasField()
        ));

        // when
        final List<Message> processed = extractMessagesFromMessageCollection(interpreter.process(messageWithNumField(Long.valueOf(5))));

        // then
        assertThat(processed)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(m -> {
                    assertThat(m.processingErrors()).isEmpty();
                    assertThat(m.getField("num_sqr")).isEqualTo(Double.valueOf(25.0));
                });
    }

    private Message messageWithNumField(Object numValue) {
        final Message msg = messageInDefaultStream("message", "test");
        msg.addField("num", numValue);
        return msg;
    }

    private List<Message> extractMessagesFromMessageCollection(Messages messages) {
        return ((MessageCollection) messages).source();
    }

    private Message messageInDefaultStream(String message, String source) {
        final Message msg = messageFactory.createMessage(message, source, Tools.nowUTC());

        final Stream mockedStream = mock(Stream.class);
        when(mockedStream.getId()).thenReturn(DEFAULT_STREAM_ID);
        msg.addStream(mockedStream);

        return msg;
    }
}
