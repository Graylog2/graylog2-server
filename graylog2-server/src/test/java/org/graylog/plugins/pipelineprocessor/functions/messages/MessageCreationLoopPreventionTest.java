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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigDto;
import org.graylog.plugins.pipelineprocessor.db.RuleMetricsConfigService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.functions.conversion.LongConversion;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Messages;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Test;

import jakarta.inject.Provider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessageCreationLoopPreventionTest extends BaseParserTest {


    final CloneMessage cloneMessage = spy(new CloneMessage());
    PipelineInterpreter pipelineInterpreter;

    @Before
    @SuppressForbidden("Allow using default thread factory")
    public void createPipelineInterpreter() {
        // load rule from resource file
        final RuleService ruleService = mock(RuleService.class);
        when(ruleService.loadAll()).thenReturn(Collections.singleton(
                RuleDao.create("r1",
                        "title",
                        "description",
                        ruleForTest(),
                        Tools.nowUTC(),
                        null, null, null)
        ));

        final PipelineService pipelineService = mock(MongoDbPipelineService.class);
        when(pipelineService.loadAll()).thenReturn(Collections.singleton(
                PipelineDao.create("p1", "title", "description",
                        """
                                pipeline "pipeline"
                                stage 0 match all
                                    rule "test rule";
                                end
                                """,
                        Tools.nowUTC(),
                        null)
        ));

        final MessageQueueAcknowledger messageQueueAcknowledger = mock(MessageQueueAcknowledger.class);
        final EventBus eventBus = mock(EventBus.class);

        final RuleMetricsConfigService ruleMetricsConfigService = mock(RuleMetricsConfigService.class);
        when(ruleMetricsConfigService.get()).thenReturn(RuleMetricsConfigDto.createDefault());
        final PipelineStreamConnectionsService pipelineStreamConnectionsService = mock(MongoDbPipelineStreamConnectionsService.class);
        final Set<String> pipelineIds = pipelineService.loadAll().stream().map(PipelineDao::id).collect(Collectors.toSet());
        final PipelineConnections pipelineConnections = PipelineConnections.create("p1", defaultStream.getId(), pipelineIds);
        when(pipelineStreamConnectionsService.loadAll()).thenReturn(Collections.singleton(pipelineConnections));

        Stream otherStream = mock(Stream.class, "Other Stream");
        when(otherStream.isPaused()).thenReturn(false);
        when(otherStream.getTitle()).thenReturn("other stream");
        when(otherStream.getId()).thenReturn("Other Stream");
        StreamService streamService = mock(StreamService.class);
        when(streamService.loadAll()).thenReturn(Lists.newArrayList(defaultStream, otherStream));
        when(streamService.loadAllEnabled()).thenReturn(Lists.newArrayList(defaultStream, otherStream));
        StreamCacheService streamCacheService = new StreamCacheService(eventBus, streamService, null);
        streamCacheService.startAsync().awaitRunning();
        final Provider<Stream> defaultStreamProvider = () -> defaultStream;
        final Map<String, Function<?>> functions = ImmutableMap.of(
                CloneMessage.NAME, cloneMessage,
                HasField.NAME, new HasField(),
                SetField.NAME, new SetField(),
                LongConversion.NAME, new LongConversion(),
                RemoveFromStream.NAME, new RemoveFromStream(streamCacheService, defaultStreamProvider),
                RouteToStream.NAME, new RouteToStream(streamCacheService, defaultStreamProvider)
        );

        final FunctionRegistry functionRegistry = new FunctionRegistry(functions);
        final PipelineRuleParser parser = new PipelineRuleParser(functionRegistry);
        final ConfigurationStateUpdater stateUpdater = new ConfigurationStateUpdater(ruleService,
                pipelineService,
                pipelineStreamConnectionsService,
                parser,
                ruleMetricsConfigService,
                new MetricRegistry(),
                Executors.newScheduledThreadPool(1),
                eventBus,
                (currentPipelines, streamPipelineConnections, ruleMetricsConfig) -> new PipelineInterpreter.State(currentPipelines, streamPipelineConnections, ruleMetricsConfig, new MetricRegistry(), 1, true)
        );
        this.pipelineInterpreter = new PipelineInterpreter(
                messageQueueAcknowledger,
                new MetricRegistry(),
                stateUpdater);
    }

    // make sure a naive call to clone_message() will not cause a loop
    @Test
    public void loopPreventionBasic() {
        Message msg = messageInDefaultStream();
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(CloneMessage.MAX_CLONES + 1)).evaluate(any(), any());
        assertEquals(CloneMessage.MAX_CLONES + 1, messages.length);
    }

    // make sure a call to clone_message() with 'preventLoops' set will stop after cloning once
    @Test
    public void loopPreventionParam() {
        Message msg = messageInDefaultStream();
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(2)).evaluate(any(), any());
        assertEquals(2, messages.length);
    }

    // make sure possible existing workarounds for loop prevention will still work
    @Test
    public void loopPreventionWorkaround1() {
        Message msg = messageInDefaultStream();
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(1)).evaluate(any(), any());
        assertEquals(2, messages.length);
    }

    // make sure possible existing workarounds for loop prevention will still work
    @Test
    public void loopPreventionWorkaround2() {
        Message msg = messageInDefaultStream();
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(2)).evaluate(any(), any());
        assertEquals(3, messages.length);
    }

    // make sure possible existing recursive calls of clone message will still work
    @Test
    public void loopPreventionRecursive() {
        Message msg = messageInDefaultStream();
        msg.addField("cycle", 5);
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(5)).evaluate(any(), any());
        assertEquals(6, messages.length);
    }

    // possible existing recursive calls which exceed MAX_CLONES will stop creating clones with an error
    @Test
    public void loopPreventionRecursiveFail() {
        Message msg = messageInDefaultStream();
        msg.addField("cycle", 110);
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(CloneMessage.MAX_CLONES + 1)).evaluate(any(), any());
        assertEquals(CloneMessage.MAX_CLONES + 1, messages.length);
    }

    // recursive calls of clone message can exceed MAX_CLONES if preventLoops is explicitly set to 'false'
    @Test
    public void loopPreventionRecursiveParam() {
        Message msg = messageInDefaultStream();
        msg.addField("cycle", 110);
        final Messages processed = pipelineInterpreter.process(msg);

        final Message[] messages = Iterables.toArray(processed, Message.class);
        verify(cloneMessage, times(110)).evaluate(any(), any());
        assertEquals(111, messages.length);
    }

    private Message messageInDefaultStream() {
        final Message msg = new Message("original message", "test", Tools.nowUTC());
        msg.addStream(defaultStream);
        return msg;
    }
}
