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
package org.graylog2.streams.metrics;

import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.metrics.InputType;
import org.graylog2.inputs.metrics.TypedInputId;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamAssociatedInputsDescriptorTest {

    // 24-hex ObjectId-shaped fixtures — the descriptor pre-filters via ObjectId::isValid.
    private static final String INPUT_A = "aaaaaaaaaaaaaaaaaaaaaa01";
    private static final String INPUT_B = "aaaaaaaaaaaaaaaaaaaaaa02";
    private static final String INPUT_C = "aaaaaaaaaaaaaaaaaaaaaa03";
    private static final String ORPHAN = "bbbbbbbbbbbbbbbbbbbbbb01";
    private static final String FORWARDER_A = "cccccccccccccccccccccc01";
    private static final String FORWARDER_B = "cccccccccccccccccccccc02";
    private static final String GHOST = "dddddddddddddddddddddd01";

    @Mock
    MoreSearch moreSearch;

    @Mock
    SearchUser searchUser;

    private StreamAssociatedInputsDescriptor descriptor;

    @BeforeEach
    void setUp() {
        descriptor = new StreamAssociatedInputsDescriptor(moreSearch, Duration.ofMinutes(5),
                Set.of(stubInputType("input", "inputs:read", Set.of(INPUT_A, INPUT_B, INPUT_C))));
    }

    @Test
    void compute_tagsEachIdWithItsType() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of("stream1", Map.of(INPUT_A, 100L, INPUT_B, 200L)));

        final List<EntityMetric<List<TypedInputId>>> result = descriptor.compute(List.of("stream1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).containsExactlyInAnyOrder(
                new TypedInputId(INPUT_A, "input"),
                new TypedInputId(INPUT_B, "input"));
    }

    @Test
    void compute_dropsIdsThatDoNotBelongToAnyRegisteredCollection() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of("stream1", Map.of(INPUT_A, 100L, ORPHAN, 5L)));

        final List<EntityMetric<List<TypedInputId>>> result = descriptor.compute(List.of("stream1"));

        assertThat(result.getFirst().value()).containsExactly(new TypedInputId(INPUT_A, "input"));
    }

    @Test
    void compute_returnsEmptyListForMissingStreams() {
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of());

        final List<EntityMetric<List<TypedInputId>>> result = descriptor.compute(List.of("stream1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().value()).isEmpty();
    }

    @Test
    void compute_skipsMalformedIdsBeforeAskingInputTypes() {
        // gl2_source_input can contain non-ObjectId garbage from message ingestion. The descriptor
        // must filter those out itself so each InputType impl can issue a direct findByIds.
        final AtomicReference<Set<String>> captured = new AtomicReference<>();
        final InputType regular = mock(InputType.class);
        lenient().when(regular.typeName()).thenReturn("input");
        lenient().when(regular.readPermission()).thenReturn("inputs:read");
        // Snapshot the arg inside the answer — the descriptor mutates `remaining` after the call,
        // so a plain ArgumentCaptor would see the post-mutation state.
        when(regular.filterMembers(any())).thenAnswer(inv -> {
            captured.set(Set.copyOf(inv.getArgument(0)));
            return Set.of(INPUT_A);
        });

        final StreamAssociatedInputsDescriptor d =
                new StreamAssociatedInputsDescriptor(moreSearch, Duration.ofMinutes(5), Set.of(regular));
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of("stream1", Map.of(INPUT_A, 1L, "not-an-object-id", 1L)));

        final List<TypedInputId> typed = d.compute(List.of("stream1")).getFirst().value();

        assertThat(typed).containsExactly(new TypedInputId(INPUT_A, "input"));
        assertThat(captured.get()).containsExactly(INPUT_A);
    }

    @Test
    void computeForUser_filtersByPerTypePermission() {
        when(searchUser.isPermitted("inputs:read", INPUT_A)).thenReturn(true);
        when(searchUser.isPermitted("inputs:read", INPUT_B)).thenReturn(false);
        when(searchUser.isPermitted("inputs:read", INPUT_C)).thenReturn(true);

        final List<TypedInputId> filtered = descriptor.computeForUser(
                List.of(
                        new TypedInputId(INPUT_A, "input"),
                        new TypedInputId(INPUT_B, "input"),
                        new TypedInputId(INPUT_C, "input")),
                searchUser);

        assertThat(filtered).containsExactly(
                new TypedInputId(INPUT_A, "input"),
                new TypedInputId(INPUT_C, "input"));
    }

    @Test
    void computeForUser_dropsIdsWithTypeFromUnregisteredInputType() {
        // If a plugin that contributed "forwarder_input" was uninstalled, cached entries with that
        // type should be filtered out — we don't know what permission to check.
        when(searchUser.isPermitted("inputs:read", INPUT_A)).thenReturn(true);

        final List<TypedInputId> filtered = descriptor.computeForUser(
                List.of(
                        new TypedInputId(INPUT_A, "input"),
                        new TypedInputId(FORWARDER_A, "forwarder_input")),
                searchUser);

        // "forwarder_input" type is not registered in this test fixture.
        assertThat(filtered).containsExactly(new TypedInputId(INPUT_A, "input"));
    }

    @Test
    void compute_multipleInputTypes_partitionsIdsAcrossCollections() {
        final InputType regular = stubInputType("input", "inputs:read", Set.of(INPUT_A));
        final InputType forwarder = stubInputType("forwarder_input", "forwarderinputs:read", Set.of(FORWARDER_B));
        final StreamAssociatedInputsDescriptor multiType =
                new StreamAssociatedInputsDescriptor(moreSearch, Duration.ofMinutes(5), Set.of(regular, forwarder));
        when(moreSearch.aggregateGroupedTerms(
                anyString(), any(RelativeRange.class),
                anyString(), anyString(), anyInt(), anyInt(),
                anyCollection()))
                .thenReturn(Map.of("stream1", Map.of(INPUT_A, 1L, FORWARDER_B, 1L, GHOST, 1L)));

        final List<TypedInputId> typed = multiType.compute(List.of("stream1")).getFirst().value();

        assertThat(typed).containsExactlyInAnyOrder(
                new TypedInputId(INPUT_A, "input"),
                new TypedInputId(FORWARDER_B, "forwarder_input"));
    }

    private static InputType stubInputType(String typeName, String readPermission, Set<String> knownIds) {
        final InputType inputType = mock(InputType.class);
        lenient().when(inputType.typeName()).thenReturn(typeName);
        lenient().when(inputType.readPermission()).thenReturn(readPermission);
        lenient().when(inputType.filterMembers(any())).thenReturn(knownIds);
        return inputType;
    }
}
