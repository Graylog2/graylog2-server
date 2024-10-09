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
package org.graylog2.outputs.filter;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.EventBus;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.outputs.filter.functions.RemoveFromStreamDestination;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.SuppressForbidden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
class PipelineRuleOutputFilterTest {

    @Mock
    private PipelineRuleOutputFilterStateUpdater stateUpdater;
    @Mock
    private Stream defaultStream;
    @Mock
    private StreamCacheService streamCacheService;

    @BeforeEach
    void setUp() {
        lenient().when(defaultStream.getId()).thenReturn("000000000000000000000001");
        lenient().when(streamCacheService.getById("000000000000000000000001")).thenReturn(defaultStream);
    }

    @Test
    void applyWithNoFilterAndOneDestination(MessageFactory messageFactory) {
        final var filter = createFilter(Map.of(), Set.of("indexer"));
        final var message = messageFactory.createMessage("msg", "src", Tools.nowUTC());
        message.addStream(defaultStream);

        final var filteredMessage = filter.apply(message);

        assertThat(filteredMessage.message()).isEqualTo(ImmutableMessage.wrap(message));
        assertThat(filteredMessage.destinations().keySet()).containsExactlyInAnyOrder("indexer");
        assertThat(filteredMessage.destinations().get("indexer")).containsExactlyInAnyOrder(defaultStream);
    }

    @Test
    void applyWithNoFilterAndTwoDestinations(MessageFactory messageFactory) {
        final var filter = createFilter(Map.of(), Set.of("indexer", "other"));
        final var message = messageFactory.createMessage("msg", "src", Tools.nowUTC());
        message.addStream(defaultStream);

        final var filteredMessage = filter.apply(message);

        assertThat(filteredMessage.message()).isEqualTo(ImmutableMessage.wrap(message));
        assertThat(filteredMessage.destinations().keySet()).containsExactlyInAnyOrder("indexer", "other");
        assertThat(filteredMessage.destinations().get("indexer")).containsExactlyInAnyOrder(defaultStream);
        assertThat(filteredMessage.destinations().get("other")).containsExactlyInAnyOrder(defaultStream);
    }

    @Test
    void applyWithFilter(MessageFactory messageFactory) {
        final var filterRules = List.of(
                RuleDao.builder()
                        .id("668cff8ed9f9636b4b629c29")
                        .title("[668cff8ed9f9636b4b629c29] Test Filter 1")
                        .source("""
                                rule "[668cff8ed9f9636b4b629c29] Test Filter 1"
                                when has_field(field : "source")
                                then
                                  __remove_from_stream_destination__(stream_id : "000000000000000000000001", destination_type : "indexer");
                                end
                                """)
                        .build()
        );

        final var pipeline = Pipeline.builder()
                .id(defaultStream.getId()) // Must be the stream ID to make PipelineRuleOutputFilterState#getPipelinesForMessage work
                .name("Test")
                .stages(ImmutableSortedSet.of(
                        Stage.builder()
                                .stage(0)
                                .match(Stage.Match.EITHER)
                                .ruleReferences(filterRules.stream().map(RuleDao::title).toList())
                                .build()
                ))
                .build();

        final var resolver = createResolver(filterRules);
        final var pipelines = resolver.resolveFunctions(Set.of(pipeline), PipelineMetricRegistry.create(new MetricRegistry(), "P", "R"));

        final var filter = createFilter(pipelines, Set.of("indexer", "other"));
        final var message = messageFactory.createMessage("msg", "src", Tools.nowUTC());
        message.addStream(defaultStream);

        final var filteredMessage = filter.apply(message);

        assertThat(filteredMessage.message()).isEqualTo(ImmutableMessage.wrap(message));
        // The filter rules removes the default stream from the indexer destination, so it shouldn't be in the
        // destinations after running the filter.
        assertThat(filteredMessage.destinations().keySet()).containsExactlyInAnyOrder("other");
        assertThat(filteredMessage.destinations().get("indexer")).isEmpty();
        assertThat(filteredMessage.destinations().get("other")).containsExactlyInAnyOrder(defaultStream);
    }

    @SuppressForbidden("Executors.newSingleThreadScheduledExecutor is okay in tests")
    private PipelineRuleOutputFilter createFilter(Map<String, Pipeline> pipelines, Set<String> destinations) {
        doAnswer((Answer<Void>) invocation -> {
            final AtomicReference<PipelineRuleOutputFilterState> state = invocation.getArgument(0);
            state.set(new PipelineRuleOutputFilterState(
                    ImmutableMap.copyOf(pipelines),
                    ImmutableSet.copyOf(destinations),
                    ImmutableSet.of(),
                    new MetricRegistry(),
                    1,
                    true
            ));
            return null;
        }).when(stateUpdater).init(any());

        return new PipelineRuleOutputFilter(
                stateUpdater,
                Executors.newSingleThreadScheduledExecutor(),
                new MetricRegistry(),
                new EventBus()
        );
    }

    private PipelineResolver createResolver(List<RuleDao> rules) {
        final var ruleParser = new PipelineRuleParser(new FunctionRegistry(
                Map.of(HasField.NAME, new HasField()),
                Map.of(RemoveFromStreamDestination.NAME, new RemoveFromStreamDestination(streamCacheService))
        ));

        return new PipelineResolver(ruleParser, PipelineResolverConfig.of(rules::stream, java.util.stream.Stream::of));
    }
}
