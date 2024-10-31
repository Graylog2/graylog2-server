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
package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldTypesLookupTest {


    @Test
    void returnsEmptyOptionalIfFieldTypesAreEmpty() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices();
        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(Collections.singleton("SomeStream"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfStreamsAreEmpty() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices();
        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(Collections.emptySet(), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfMultipleTypesExistForField() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "float")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfNoTypesExistForStream() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(Collections.singleton("stream2"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInStream() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).contains("long");
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInAllStreams() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        final Optional<String> result = lookup.getType(ImmutableSet.of("stream1", "stream2"), "somefield");
        assertThat(result).contains("long");
    }

    @Test
    void getTypesReturnsEmptyMapIfFieldTypesAreEmpty() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices();
        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        assertThat(lookup.getTypes(Set.of("SomeStream"), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapIfStreamsAreEmpty() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices();
        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        assertThat(lookup.getTypes(Set.of(), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapIfMultipleTypesExistForField() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "float")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        assertThat(lookup.getTypes(Set.of("stream1"), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapForNonExistingFields() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("existing_field", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", ImmutableSet.of(
                        FieldTypeDTO.create("existing_field", "long")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());
        assertThat(lookup.getTypes(Set.of("stream1", "stream2"), Set.of("somefield1", "somefield2"))).isEmpty();
    }

    @Test
    void getTypesBehavesCorrectlyIfMultipleScenariosAreMixed() {
        final Pair<IndexFieldTypesService, StreamService> services = mockServices(
                IndexFieldTypesDTO.create("indexSet1", "stream1", Set.of(
                        FieldTypeDTO.create("good_field", "long"),
                        FieldTypeDTO.create("bad_field", "ip")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", Set.of(
                        FieldTypeDTO.create("good_field", "long"),
                        FieldTypeDTO.create("bad_field", "text")
                ))
        );

        final FieldTypesLookup lookup = new FieldTypesLookup(services.getLeft(), services.getRight());

        assertThat(lookup.getTypes(Set.of("stream1", "stream2"), Set.of("good_field", "bad_field", "unknown_field")))
                .satisfies(result -> assertThat(result).doesNotContainKey("bad_field")) //different type in different streams
                .satisfies(result -> assertThat(result).doesNotContainKey("unknown_field")) //no type info
                .satisfies(result -> assertThat(result).containsEntry("good_field", "long")); //proper type info

    }

    /**
     * @param dtos what we expect as a result of {@link IndexFieldTypesService#findForIndexSets(Collection)}
     * @return pair of services that can be used as dependencies for the {@link FieldTypesLookup].
     */
    Pair<IndexFieldTypesService, StreamService> mockServices(IndexFieldTypesDTO... dtos) {

        // caution, we interchange stream and index names in this test freely
        final Set<String> expectedStreams = Arrays.stream(dtos).map(IndexFieldTypesDTO::indexName).collect(Collectors.toSet());

        final Set<String> expectedIndexSets = Arrays.stream(dtos).map(IndexFieldTypesDTO::indexSetId).collect(Collectors.toSet());

        final StreamService streamService = mock(StreamService.class);
        final IndexFieldTypesService indexFieldTypeService = mock(IndexFieldTypesService.class);

        Mockito.doAnswer(invocation -> Arrays.stream(dtos)
                .map(dto -> {
                    final Stream mock = mock(Stream.class, RETURNS_DEEP_STUBS);
                    when(mock.getIndexSet().getConfig().id()).thenReturn(dto.indexSetId());
                    return mock;
                })
                .collect(Collectors.toSet())
        ).when(streamService).loadByIds(expectedStreams);

        Mockito.when(indexFieldTypeService.findForIndexSets(expectedIndexSets))
                .thenReturn(Set.of(dtos));

        return Pair.of(indexFieldTypeService, streamService);
    }
}
