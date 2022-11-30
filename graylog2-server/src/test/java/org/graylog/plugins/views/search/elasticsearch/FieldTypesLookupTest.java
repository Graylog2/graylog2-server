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
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldTypesLookupTest {
    private IndexFieldTypesService indexFieldTypesService;
    private FieldTypesLookup fieldTypesLookup;

    @BeforeEach
    void setUp() {
        this.indexFieldTypesService = mock(IndexFieldTypesService.class);
        this.fieldTypesLookup = new FieldTypesLookup(indexFieldTypesService);
    }

    @Test
    void returnsEmptyOptionalIfFieldTypesAreEmpty() {
        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("SomeStream"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfStreamsAreEmpty() {
        final Optional<String> result = this.fieldTypesLookup.getType(Collections.emptySet(), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfMultipleTypesExistForField() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "float")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyOptionalIfNoTypesExistForStream() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream2"), "somefield");
        assertThat(result).isEmpty();
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInStream() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(Collections.singleton("stream1"), "somefield");
        assertThat(result).contains("long");
    }

    @Test
    void returnsFieldTypeIfSingleTypeExistsForFieldInAllStreams() {
        when(this.indexFieldTypesService.findForStreamIds(ImmutableSet.of("stream1", "stream2"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                ))
        ));

        final Optional<String> result = this.fieldTypesLookup.getType(ImmutableSet.of("stream1", "stream2"), "somefield");
        assertThat(result).contains("long");
    }

    @Test
    void getTypesReturnsEmptyMapIfFieldTypesAreEmpty() {
        assertThat(fieldTypesLookup.getTypes(Set.of("SomeStream"), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapIfStreamsAreEmpty() {
        assertThat(fieldTypesLookup.getTypes(Set.of(), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapIfMultipleTypesExistForField() {
        when(this.indexFieldTypesService.findForStreamIds(Collections.singleton("stream1"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("somefield", "float")
                ))
        ));

        assertThat(fieldTypesLookup.getTypes(Set.of("stream1"), Set.of("somefield"))).isEmpty();
    }

    @Test
    void getTypesReturnsEmptyMapForNonExistingFields() {
        when(this.indexFieldTypesService.findForStreamIds(ImmutableSet.of("stream1", "stream2"))).thenReturn(ImmutableSet.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", ImmutableSet.of(
                        FieldTypeDTO.create("existing_field", "long")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", ImmutableSet.of(
                        FieldTypeDTO.create("existing_field", "long")
                ))
        ));

        assertThat(fieldTypesLookup.getTypes(Set.of("stream1", "stream2"), Set.of("somefield1", "somefield2"))).isEmpty();
    }

    @Test
    void getTypesBehavesCorrectlyIfMultipleScenariosAreMixed() {
        when(this.indexFieldTypesService.findForStreamIds(Set.of("stream1", "stream2"))).thenReturn(Set.of(
                IndexFieldTypesDTO.create("indexSet1", "stream1", Set.of(
                        FieldTypeDTO.create("good_field", "long"),
                        FieldTypeDTO.create("bad_field", "ip")
                )),
                IndexFieldTypesDTO.create("indexSet2", "stream2", Set.of(
                        FieldTypeDTO.create("good_field", "long"),
                        FieldTypeDTO.create("bad_field", "text")
                ))
        ));

        assertThat(fieldTypesLookup.getTypes(Set.of("stream1", "stream2"), Set.of("good_field", "bad_field", "unknown_field")))
                .satisfies(result -> assertThat(result).doesNotContainKey("bad_field")) //different type in different streams
                .satisfies(result -> assertThat(result).doesNotContainKey("unknown_field")) //no type info
                .satisfies(result -> assertThat(result).containsEntry("good_field", "long")); //proper type info

    }
}
