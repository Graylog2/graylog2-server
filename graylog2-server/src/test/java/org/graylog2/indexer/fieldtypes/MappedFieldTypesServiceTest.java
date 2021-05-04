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
package org.graylog2.indexer.fieldtypes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.elasticsearch.IndexLookup;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MappedFieldTypesServiceTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamService streamService;

    @Mock
    private IndexFieldTypesService indexFieldTypesService;

    @Mock
    private IndexLookup indexLookup;

    @Captor
    private ArgumentCaptor<Set<String>> streamIdCaptor;

    @Captor
    private ArgumentCaptor<TimeRange> timeRangeCaptor;

    private MappedFieldTypesService mappedFieldTypesService;

    @Before
    public void setUp() throws Exception {
        this.mappedFieldTypesService = new MappedFieldTypesService(streamService, indexFieldTypesService, new FieldTypeMapper(), indexLookup);
        when(streamService.indexSetIdsByIds(Collections.singleton("stream1"))).thenReturn(Collections.singleton("indexSetId"));
    }

    @Test
    public void fieldsOfSameTypeDoNotReturnCompoundTypeIfPropertiesAreDifferent() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.create("field1", "keyword"),
                        FieldTypeDTO.create("field2", "long")
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.create("field1", "text"),
                        FieldTypeDTO.create("field2", "long")
                )
        );
        when(indexFieldTypesService.findForIndexSets(Collections.singleton("indexSetId"))).thenReturn(fieldTypes);
        when(indexLookup.indexNamesForStreamsInTimeRange(Collections.singleton("stream1"), RelativeRange.allTime())).thenReturn(ImmutableSet.of("testIndex", "testIndex2"));

        final Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), RelativeRange.allTime());
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))),
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("compound")))
        );
    }

    @Test
    public void fieldsOfDifferentTypesDoReturnCompoundType() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.create("field1", "long"),
                        FieldTypeDTO.create("field2", "long")
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.create("field1", "text"),
                        FieldTypeDTO.create("field2", "long")
                )
        );
        when(indexFieldTypesService.findForIndexSets(Collections.singleton("indexSetId"))).thenReturn(fieldTypes);
        when(indexLookup.indexNamesForStreamsInTimeRange(Collections.singleton("stream1"), RelativeRange.allTime())).thenReturn(ImmutableSet.of("testIndex", "testIndex2"));

        final Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), RelativeRange.allTime());
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))),
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("compound(long,string)", ImmutableSet.of("compound")))
        );
    }

    @Test
    public void requestsFieldTypesForRequestedTimeRange() throws Exception {
        this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), AbsoluteRange.create("2010-05-17T23:28:14.000+02:00", "2021-05-05T12:09:23.213+02:00"));

        verify(this.indexLookup, times(1)).indexNamesForStreamsInTimeRange(streamIdCaptor.capture(), timeRangeCaptor.capture());

        assertThat(streamIdCaptor.getValue()).containsExactly("stream1");
        assertThat(timeRangeCaptor.getValue()).isEqualTo(AbsoluteRange.create("2010-05-17T23:28:14.000+02:00", "2021-05-05T12:09:23.213+02:00"));
    }

    private IndexFieldTypesDTO createIndexTypes(String indexId, String indexName, FieldTypeDTO... fieldTypes) {
        return IndexFieldTypesDTO.create(indexId, indexName, java.util.stream.Stream.of(fieldTypes).collect(Collectors.toSet()));
    }
}
