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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.journal.Journal;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;

import static com.google.common.collect.Sets.newHashSet;
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

        final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(MongoDbPipelineStreamConnectionsService.class);
        final PipelineConnections pipelineConnections = PipelineConnections.create(null,
                                                                                                  "default",
                                                                                                  newHashSet("cde"));
        when(pipelineStreamConnectionsService.loadAll()).thenReturn(
                newHashSet(pipelineConnections)
        );

        final Map<String, Function<?>> functions = Maps.newHashMap();
        functions.put(CreateMessage.NAME, new CreateMessage());
        functions.put(StringConversion.NAME, new StringConversion());

        final PipelineRuleParser parser = setupParser(functions);

        final PipelineInterpreter interpreter = new PipelineInterpreter(
                ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                mock(Journal.class),
                new MetricRegistry(),
                Executors.newScheduledThreadPool(1),
                mock(EventBus.class)
        );

        Message msg = new Message("original message", "test", Tools.nowUTC());
        final Messages processed = interpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        assertEquals(2, messages.length);
    }

    private PipelineRuleParser setupParser(Map<String, Function<?>> functions) {
        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        return new PipelineRuleParser(functionRegistry);
    }

}