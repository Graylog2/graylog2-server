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

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.codegen.CodeGenerator;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryPipelineService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.memory.InMemoryRuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.journal.Journal;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.Executors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineInterpreterTest {

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
                PipelineDao.create("cde", "title", "description",
                        "pipeline \"pipeline\"\n" +
                                "stage 0 match all\n" +
                                "    rule \"creates message\";\n" +
                                "end\n",
                        Tools.nowUTC(),
                        null)
        ));

        final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(
                MongoDbPipelineStreamConnectionsService.class);
        final PipelineConnections pipelineConnections = PipelineConnections.create(null,
                DEFAULT_STREAM_ID,
                newHashSet("cde"));
        when(pipelineStreamConnectionsService.loadAll()).thenReturn(
                newHashSet(pipelineConnections)
        );

        final Map<String, Function<?>> functions = Maps.newHashMap();
        functions.put(CreateMessage.NAME, new CreateMessage());
        functions.put(StringConversion.NAME, new StringConversion());

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry, new CodeGenerator());

        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                new MetricRegistry(),
                functionRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class),
                (currentPipelines, streamPipelineConnections) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, new MetricRegistry(), 1, true));
        final PipelineInterpreter interpreter = new PipelineInterpreter(
                mock(Journal.class),
                new MetricRegistry(),
                mock(EventBus.class),
                stateUpdater
        );

        Message msg = messageInDefaultStream("original message", "test");
        final Messages processed = interpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        assertEquals(2, messages.length);
    }

    @Test
    public void testMetrics() {
        final RuleService ruleService = new InMemoryRuleService();
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

        final PipelineService pipelineService = new InMemoryPipelineService();
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

        final PipelineStreamConnectionsService pipelineStreamConnectionsService = new InMemoryPipelineStreamConnectionsService();
        pipelineStreamConnectionsService.save(PipelineConnections.create(null,
                DEFAULT_STREAM_ID,
                newHashSet("cde")));

        final Map<String, Function<?>> functions = Maps.newHashMap();

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry, new CodeGenerator());

        final MetricRegistry metricRegistry = new MetricRegistry();
        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                metricRegistry,
                functionRegistry,
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class), (currentPipelines, streamPipelineConnections) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, new MetricRegistry(), 1, true));
        final PipelineInterpreter interpreter = new PipelineInterpreter(
                mock(Journal.class),
                metricRegistry,
                mock(EventBus.class),
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