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
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.StreamCacheService;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.parser.RuleBuilderService;
import org.graylog.testing.messages.MessagesExtension;
import org.graylog2.outputs.filter.functions.RemoveFromStreamDestination;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.filters.StreamDestinationFilterRuleDTO;
import org.graylog2.streams.filters.StreamDestinationFilterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MessagesExtension.class)
class PipelineRuleOutputFilterStateUpdaterTest {

    @Mock
    private StreamDestinationFilterService filterService;
    @Mock
    private RuleBuilderService ruleBuilderService;
    @Mock
    private PipelineResolver.Factory resolverFactory;
    @Mock
    private PipelineRuleParser ruleParser;
    @Mock
    private StreamCacheService streamCacheService;
    @Mock
    private Stream defaultStream;
    @Mock
    private Stream testStream;

    @BeforeEach
    void setUp() {
        lenient().when(defaultStream.getId()).thenReturn("000000000000000000000001");
        lenient().when(testStream.getId()).thenReturn("668cff9ed9f9636b4b629c37");
    }

    private String ruleTitle(StreamDestinationFilterRuleDTO dto) {
        return f("[%s] %s", dto.id(), dto.title());
    }

    @Test
    void init(MessageFactory messageFactory) {
        final var defaultStreamDestinationFilter = StreamDestinationFilterRuleDTO.builder()
                .id("54e3deadbeefdeadbeef0001")
                .title("Test 1")
                .streamId(defaultStream.getId())
                .destinationType("indexer")
                .rule(RuleBuilder.builder().build()) // Not needed because we mock the RuleBuilderService
                .build();
        final var testStreamDestinationFilter = StreamDestinationFilterRuleDTO.builder()
                .id("54e3deadbeefdeadbeef0002")
                .title("Test 2")
                .streamId(testStream.getId())
                .destinationType("indexer")
                .rule(RuleBuilder.builder().build()) // Not needed because we mock the RuleBuilderService
                .build();
        final var defaultStreamRuleDao = RuleDao.builder()
                .id(defaultStreamDestinationFilter.id())
                .title("does-not-matter")
                .source("""
                        rule "%s"
                        when has_field(field : "source")
                        then
                          __remove_from_stream_destination__(stream_id : "%s", destination_type : "indexer");
                        end
                        """.formatted(ruleTitle(defaultStreamDestinationFilter), defaultStream.getId()))
                .build();
        final var testStreamRuleDao = RuleDao.builder()
                .id(testStreamDestinationFilter.id())
                .title("does-not-matter")
                .source("""
                        rule "%s"
                        when has_field(field : "source")
                        then
                          __remove_from_stream_destination__(stream_id : "%s", destination_type : "indexer");
                        end
                        """.formatted(ruleTitle(testStreamDestinationFilter), testStream.getId()))
                .build();

        doAnswer((Answer<Void>) invocation -> {
            final Consumer<StreamDestinationFilterService.GroupByStreamResult> consumer = invocation.getArgument(0);
            // The consumer must be called once for each stream group
            consumer.accept(new StreamDestinationFilterService.GroupByStreamResult(defaultStream.getId(), Set.of(defaultStreamDestinationFilter)));
            consumer.accept(new StreamDestinationFilterService.GroupByStreamResult(testStream.getId(), Set.of(testStreamDestinationFilter)));
            return null;
        }).when(filterService).forEachEnabledFilterGroupedByStream(any());

        when(resolverFactory.create(any(), any())).thenReturn(createResolver(List.of(defaultStreamRuleDao, testStreamRuleDao)));

        // Mock each rule source generator call to return the correct source for the given rule
        when(ruleBuilderService.generateRuleSource(eq(ruleTitle(defaultStreamDestinationFilter)), any(RuleBuilder.class), anyBoolean()))
                .thenReturn(defaultStreamRuleDao.source());
        when(ruleBuilderService.generateRuleSource(eq(ruleTitle(testStreamDestinationFilter)), any(RuleBuilder.class), anyBoolean()))
                .thenReturn(testStreamRuleDao.source());

        final var stateUpdater = new PipelineRuleOutputFilterStateUpdater(
                filterService,
                (pipelines, destinations, activeStreams) -> new PipelineRuleOutputFilterState(
                        pipelines,
                        destinations,
                        activeStreams,
                        new MetricRegistry(),
                        1,
                        true
                ),
                Map.of("indexer", mock(FilteredMessageOutput.class)),
                ruleBuilderService,
                resolverFactory,
                ruleParser,
                new MetricRegistry()
        );

        final var activeState = new AtomicReference<PipelineRuleOutputFilterState>();
        stateUpdater.init(activeState);

        assertThat(activeState.get()).isNotNull().satisfies(state -> {
            assertThat(state.isEmpty()).isFalse();
            assertThat(state.getCurrentPipelines().keySet()).containsExactlyInAnyOrder(defaultStream.getId(), testStream.getId());
            assertThat(state.getDestinations()).containsExactlyInAnyOrder("indexer");
            assertThat(state.getActiveStreams()).containsExactlyInAnyOrder(defaultStream.getId(), testStream.getId());

            // Asser default stream pipeline
            assertThat(state.getCurrentPipelines().get(defaultStream.getId())).isNotNull().satisfies(pipeline -> {
                assertThat(pipeline.id()).isEqualTo(defaultStream.getId());
                assertThat(pipeline.name()).isEqualTo("Stream Destination Filter: " + defaultStream.getId());
                assertThat(pipeline.stages()).hasSize(1);
                assertThat(pipeline.stages().first()).satisfies(stage -> {
                    assertThat(stage.stage()).isEqualTo(0);
                    assertThat(stage.getRules()).hasSize(1).first().satisfies(rule -> {
                        assertThat(rule.id()).isEqualTo(defaultStreamDestinationFilter.id());
                        assertThat(rule.name()).isEqualTo(ruleTitle(defaultStreamDestinationFilter));
                    });
                    assertThat(stage.ruleReferences())
                            .containsExactlyInAnyOrder(ruleTitle(defaultStreamDestinationFilter));
                });
            });

            // Asser test stream pipeline
            assertThat(state.getCurrentPipelines().get(testStream.getId())).isNotNull().satisfies(pipeline -> {
                assertThat(pipeline.id()).isEqualTo(testStream.getId());
                assertThat(pipeline.name()).isEqualTo("Stream Destination Filter: " + testStream.getId());
                assertThat(pipeline.stages()).hasSize(1);
                assertThat(pipeline.stages().first()).satisfies(stage -> {
                    assertThat(stage.stage()).isEqualTo(0);
                    assertThat(stage.getRules()).hasSize(1).first().satisfies(rule -> {
                        assertThat(rule.id()).isEqualTo(testStreamDestinationFilter.id());
                        assertThat(rule.name()).isEqualTo(ruleTitle(testStreamDestinationFilter));
                    });
                    assertThat(stage.ruleReferences())
                            .containsExactlyInAnyOrder(ruleTitle(testStreamDestinationFilter));
                });
            });

            final var message = messageFactory.createMessage("message", "source", Tools.nowUTC());

            // The message doesn't have a stream, so we shouldn't get any pipelines
            assertThat(state.getPipelinesForMessage(message)).isEmpty();

            // Add the default stream to test that we get the default stream pipeline
            message.addStream(defaultStream);
            assertThat(state.getPipelinesForMessage(message))
                    .hasSize(1)
                    .containsExactlyInAnyOrder(state.getCurrentPipelines().get(defaultStream.getId()));

            // Add the test stream to test that we now get both pipelines
            message.addStream(testStream);
            assertThat(state.getPipelinesForMessage(message))
                    .hasSize(2)
                    .containsAll(state.getCurrentPipelines().values());

            // Remove default stream to test that we now only get the test stream pipeline
            message.removeStream(defaultStream);
            assertThat(state.getPipelinesForMessage(message))
                    .hasSize(1)
                    .containsExactlyInAnyOrder(state.getCurrentPipelines().get(testStream.getId()));
        });
    }

    private PipelineResolver createResolver(List<RuleDao> rules) {
        final var ruleParser = new PipelineRuleParser(new FunctionRegistry(
                Map.of(HasField.NAME, new HasField()),
                Map.of(RemoveFromStreamDestination.NAME, new RemoveFromStreamDestination(streamCacheService))
        ));

        return new PipelineResolver(ruleParser, PipelineResolverConfig.of(rules::stream, java.util.stream.Stream::of));
    }
}
