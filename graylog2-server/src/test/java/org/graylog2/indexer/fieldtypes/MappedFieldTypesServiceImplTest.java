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
import org.graylog2.Configuration;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MappedFieldTypesServiceImplTest {
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
        this.mappedFieldTypesService = new MappedFieldTypesServiceImpl(new Configuration(), streamService, indexFieldTypesService, new FieldTypeMapper(), indexLookup);
        when(streamService.indexSetIdsByIds(Collections.singleton("stream1"))).thenReturn(Collections.singleton("indexSetId"));
        when(streamService.indexSetIdsByIds(Collections.singleton("stream2"))).thenReturn(Collections.singleton("indexSetId"));
    }

    @Test
    public void testDifferenceBetweenStreamAwareAndUnawareFieldTypeRetrieval() {
        final Configuration withStreamAwarenessOn = spy(new Configuration());
        doReturn(true).when(withStreamAwarenessOn).maintainsStreamBasedFieldLists();
        MappedFieldTypesServiceImpl streamAwareMappedFieldTypesService = new MappedFieldTypesServiceImpl(withStreamAwarenessOn, streamService, indexFieldTypesService, new FieldTypeMapper(), indexLookup);

        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("keyword").streams(Set.of("stream1")).build(),
                        FieldTypeDTO.builder().fieldName("field3").physicalType("keyword").streams(Set.of("stream1")).build()
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("keyword").streams(Set.of("stream1", "stream2")).build(),
                        FieldTypeDTO.builder().fieldName("field2").physicalType("keyword").streams(Set.of("stream2")).build(),
                        FieldTypeDTO.builder().fieldName("field4").physicalType("keyword").streams(Set.of("stream1")).build()
                )
        );
        when(indexFieldTypesService.findForIndexSets(Collections.singleton("indexSetId"))).thenReturn(fieldTypes);
        when(indexLookup.indexNamesForStreamsInTimeRange(Collections.singleton("stream1"), RelativeRange.allTime())).thenReturn(ImmutableSet.of("testIndex", "testIndex2"));
        when(indexLookup.indexNamesForStreamsInTimeRange(Collections.singleton("stream2"), RelativeRange.allTime())).thenReturn(ImmutableSet.of("testIndex2"));

        Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), RelativeRange.allTime());
        //All fields are expected
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field3", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field4", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable")))
        );

        result = streamAwareMappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), RelativeRange.allTime());
        //Stream-aware approach excludes field2, as it is present in stream2 only
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field3", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field4", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable")))
        );

        result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream2"), RelativeRange.allTime());
        //Field3 is excludes, as it is present only in "testIndex", ignored during indexLookup phase
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field4", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable")))
        );

        //Stream-aware approach excludes, additionally to field3, field4 as well, as it is present in stream1 only
        result = streamAwareMappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream2"), RelativeRange.allTime());
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable"))),
                MappedFieldTypeDTO.create("field2", FieldTypes.Type.createType("string", ImmutableSet.of("enumerable")))
        );
    }

    @Test
    public void fieldsOfSameTypeDoNotReturnCompoundTypeIfPropertiesAreDifferent() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("keyword").streams(Set.of("stream1")).build()
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("text").streams(Set.of("stream1")).build()
                )
        );
        when(indexFieldTypesService.findForIndexSets(Collections.singleton("indexSetId"))).thenReturn(fieldTypes);
        when(indexLookup.indexNamesForStreamsInTimeRange(Collections.singleton("stream1"), RelativeRange.allTime())).thenReturn(ImmutableSet.of("testIndex", "testIndex2"));

        final Set<MappedFieldTypeDTO> result = this.mappedFieldTypesService.fieldTypesByStreamIds(Collections.singleton("stream1"), RelativeRange.allTime());
        assertThat(result).containsExactlyInAnyOrder(
                MappedFieldTypeDTO.create("field1", FieldTypes.Type.createType("string", ImmutableSet.of()))
        );
    }

    @Test
    public void fieldsOfDifferentTypesDoReturnCompoundType() {
        final List<IndexFieldTypesDTO> fieldTypes = ImmutableList.of(
                createIndexTypes(
                        "deadbeef",
                        "testIndex",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("long").streams(Set.of("stream1")).build(),
                        FieldTypeDTO.builder().fieldName("field2").physicalType("long").streams(Set.of("stream1")).build()
                ),
                createIndexTypes(
                        "affeaffe",
                        "testIndex2",
                        FieldTypeDTO.builder().fieldName("field1").physicalType("text").streams(Set.of("stream1")).build(),
                        FieldTypeDTO.builder().fieldName("field2").physicalType("long").streams(Set.of("stream1")).build()
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
