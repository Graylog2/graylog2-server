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
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.db.PipelineSourceService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamAssignmentService;
import org.graylog.plugins.pipelineprocessor.db.RuleSourceService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog.plugins.pipelineprocessor.functions.StringCoercion;
import org.graylog.plugins.pipelineprocessor.functions.messages.CreateMessage;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.rest.PipelineSource;
import org.graylog.plugins.pipelineprocessor.rest.PipelineStreamAssignment;
import org.graylog.plugins.pipelineprocessor.rest.RuleSource;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.journal.Journal;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineInterpreterTest {

    @Test
    public void testCreateMessage() {
        final RuleSourceService ruleSourceService = mock(RuleSourceService.class);
        when(ruleSourceService.loadAll()).thenReturn(Collections.singleton(
                RuleSource.create("abc",
                                  "title",
                                  "description",
                                  "rule \"creates message\"\n" +
                                          "when string(message.`message`) == \"original message\"\n" +
                                          "then\n" +
                                          "  create_message(\"derived message\");\n" +
                                          "end",
                                  Tools.nowUTC(),
                                  null)
        ));

        final PipelineSourceService pipelineSourceService = mock(PipelineSourceService.class);
        when(pipelineSourceService.loadAll()).thenReturn(Collections.singleton(
                PipelineSource.create("cde", "title", "description",
                                      "pipeline \"pipeline\"\n" +
                                              "stage 0 match all\n" +
                                              "    rule \"creates message\";\n" +
                                              "end\n",
                                      Tools.nowUTC(),
                                      null)
        ));

        final Map<String, Function<?>> functions = Maps.newHashMap();
        functions.put(CreateMessage.NAME, new CreateMessage());
        functions.put(StringCoercion.NAME, new StringCoercion());

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);

        final PipelineStreamAssignmentService pipelineStreamAssignmentService = mock(PipelineStreamAssignmentService.class);
        final PipelineStreamAssignment pipelineStreamAssignment = PipelineStreamAssignment.create(null,
                                                                                                  "default",
                                                                                                  newHashSet("cde"));
        when(pipelineStreamAssignmentService.loadAll()).thenReturn(
                newHashSet(pipelineStreamAssignment))
        ;

        final PipelineInterpreter interpreter = new PipelineInterpreter(
                ruleSourceService, pipelineSourceService, pipelineStreamAssignmentService, parser, mock(Journal.class), mock(MetricRegistry.class),
                Executors.newSingleThreadScheduledExecutor(), mock(EventBus.class)
        );
        interpreter.handlePipelineChanges(PipelinesChangedEvent.create(Collections.emptySet(),Collections.emptySet()));
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        interpreter.handlePipelineAssignmentChanges(pipelineStreamAssignment);

        Message msg = new Message("original message", "test", Tools.nowUTC());
        final Messages processed = interpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        assertEquals(2, messages.length);
    }

}