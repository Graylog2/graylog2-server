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
package org.graylog2.indexer.datastream;

import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indices.Template;
import org.graylog2.plugin.streams.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DataStreamServiceImplTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private DataStreamAdapter dataStreamAdapter;
    @Mock
    private IndexFieldTypesService indexFieldTypesService;

    private DataStreamService dataStreamService;

    @Before
    public void setUp() {
        dataStreamService = new DataStreamServiceImpl(dataStreamAdapter, indexFieldTypesService);
    }

    @Test
    public void createDataStreamPerformsFunctions() {
        final String name = "teststream";
        final String ts = "ts";
        final Map<String, Map<String, String>> mappings = new HashMap<>();
        final Policy policy = mock(Policy.class);
        dataStreamService.createDataStream(name, ts, mappings, policy);
        verify(dataStreamAdapter).ensureDataStreamTemplate(eq(name + "-template"), any(), eq(ts));
        verify(indexFieldTypesService).upsert(any());
        verify(dataStreamAdapter).createDataStream(name);
        verify(dataStreamAdapter).applyIsmPolicy(name, policy);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void templateCreatesTimestampMapping() {
        final Map<String, Map<String, String>> mappings = new HashMap<>();
        String ts = "ts";
        dataStreamService.createDataStream("teststream", ts, mappings, mock(Policy.class));
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(dataStreamAdapter).ensureDataStreamTemplate(anyString(), templateCaptor.capture(), anyString());
        HashMap<String, Object> fieldMappings = (HashMap<String, Object>) templateCaptor.getValue().mappings().get("properties");
        Map<String, String> timestampMapping = (Map<String, String>) fieldMappings.get(ts);
        assertThat(timestampMapping).isNotNull();
        assertThat(timestampMapping.get("type")).isEqualTo("date");
        assertThat(timestampMapping.get("format")).isEqualTo("yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void templateDoesNotOverwriteTimestampMapping() {
        final Map<String, Map<String, String>> mappings = new HashMap<>();
        String ts = "ts";
        mappings.put(ts, Map.of("type", "date", "format", "mycustomformat"));
        dataStreamService.createDataStream("teststream", ts, mappings, mock(Policy.class));
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(dataStreamAdapter).ensureDataStreamTemplate(anyString(), templateCaptor.capture(), anyString());
        HashMap<String, Object> fieldMappings = (HashMap<String, Object>) templateCaptor.getValue().mappings().get("properties");
        Map<String, String> timestampMapping = (Map<String, String>) fieldMappings.get(ts);
        assertThat(timestampMapping).isNotNull();
        assertThat(timestampMapping.get("type")).isEqualTo("date");
        assertThat(timestampMapping.get("format")).isEqualTo("mycustomformat");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fieldTypesCreated() {
        final Map<String, Map<String, String>> mappings = new HashMap<>();
        String customField = "field1";
        mappings.put(customField, Map.of("type", "keyword"));
        String streamName = "teststream";
        dataStreamService.createDataStream(streamName, "ts", mappings, mock(Policy.class));
        ArgumentCaptor<Template> templateCaptor = ArgumentCaptor.forClass(Template.class);
        verify(dataStreamAdapter).ensureDataStreamTemplate(anyString(), templateCaptor.capture(), anyString());
        HashMap<String, Object> fieldMappings = (HashMap<String, Object>) templateCaptor.getValue().mappings().get("properties");
        Map<String, String> timestampMapping = (Map<String, String>) fieldMappings.get(customField);
        assertThat(timestampMapping).isNotNull();
        assertThat(timestampMapping.get("type")).isEqualTo("keyword");
        ArgumentCaptor<IndexFieldTypesDTO> fieldTypes = ArgumentCaptor.forClass(IndexFieldTypesDTO.class);
        verify(indexFieldTypesService).upsert(fieldTypes.capture());
        IndexFieldTypesDTO fieldTypesDto = fieldTypes.getValue();
        assertThat(fieldTypesDto.indexName()).isEqualTo(streamName);
        assertThat(fieldTypesDto.indexSetId()).isEqualTo(Stream.DATASTREAM_PREFIX + streamName);
        assertThat(fieldTypesDto.fields()).hasSize(2);
    }

}
