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
package org.graylog2.indexer.fieldtypes.streamfiltered.filters;

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.MissingStoredStreamFieldsException;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.StoredStreamFieldsService;
import org.graylog2.indexer.fieldtypes.streamfiltered.storage.model.StoredStreamFields;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StoredSearchEngineStreamBasedFieldTypeFilterTest {

    @InjectMocks
    private StoredSearchEngineStreamBasedFieldTypeFilter toTest;
    @Mock
    private StoredStreamFieldsService storedStreamFieldsService;

    private final Set<String> indexNames = Collections.singleton("graylog_0");
    private Set<FieldTypeDTO> singleMessageField = Collections.singleton(FieldTypeDTO.create("message", "text"));

    @Test
    void returnsEmptyCollectionOnNullStreams() {
        assertThat(toTest.filterFieldTypes(singleMessageField, indexNames, null))
                .isEmpty();
    }

    @Test
    void returnsEmptyCollectionOnEmptyStreams() {
        assertThat(toTest.filterFieldTypes(singleMessageField, indexNames, Collections.emptySet()))
                .isEmpty();
    }

    @Test
    void throwsExceptionIfCannotFindStoredEntryForStream() {

        doReturn(Optional.empty()).when(storedStreamFieldsService).get("streamId");

        assertThatThrownBy(() -> toTest.filterFieldTypes(
                singleMessageField,
                indexNames,
                Collections.singleton("streamId")))
                .isInstanceOf(MissingStoredStreamFieldsException.class);
    }

    @Test
    void throwsExceptionIfFindsOutdatedStoredEntryForStream() {

        StoredStreamFields storedStreamFields = mock(StoredStreamFields.class);
        doReturn(true).when(storedStreamFields).isOutdated();
        doReturn(Optional.of(storedStreamFields)).when(storedStreamFieldsService).get("streamId");

        assertThatThrownBy(() -> toTest.filterFieldTypes(
                singleMessageField,
                indexNames,
                Collections.singleton("streamId")))
                .isInstanceOf(MissingStoredStreamFieldsException.class);
    }

    @Test
    void returnsProperFields() {
        final ImmutableSet<FieldTypeDTO> stream1Fields = ImmutableSet.of(
                FieldTypeDTO.create("count", "long"),
                FieldTypeDTO.create("message", "text")
        );
        StoredStreamFields storedStreamFieldsForStream1 = StoredStreamFields.create("stream1", stream1Fields);
        doReturn(Optional.of(storedStreamFieldsForStream1)).when(storedStreamFieldsService).get("stream1");

        final ImmutableSet<FieldTypeDTO> stream2Fields = ImmutableSet.of(
                FieldTypeDTO.create("host", "keyword"),
                FieldTypeDTO.create("message", "text"),
                FieldTypeDTO.create("does not exist in indices anymore", "text")
        );
        StoredStreamFields storedStreamFieldsForStream2 = StoredStreamFields.create("stream2", stream2Fields);
        doReturn(Optional.of(storedStreamFieldsForStream2)).when(storedStreamFieldsService).get("stream2");

        final ImmutableSet<FieldTypeDTO> fieldstoBeFiltered = ImmutableSet.of(
                FieldTypeDTO.create("count", "long"),
                FieldTypeDTO.create("message", "text"),
                FieldTypeDTO.create("host", "keyword"),
                FieldTypeDTO.create("not in any stream I", "text"),
                FieldTypeDTO.create("not in any stream II", "long")
        );
        final Set<FieldTypeDTO> filteredFields = toTest.filterFieldTypes(
                fieldstoBeFiltered,
                indexNames,
                ImmutableSet.of("stream1", "stream2"));

        assertThat(filteredFields)
                .isNotNull()
                .hasSize(3)
                .contains(FieldTypeDTO.create("count", "long"))
                .contains(FieldTypeDTO.create("host", "keyword"))
                .contains(FieldTypeDTO.create("message", "text"));
    }
}
