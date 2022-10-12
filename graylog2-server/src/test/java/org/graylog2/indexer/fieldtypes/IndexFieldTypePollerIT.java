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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.Configuration;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.MessageIndexTemplateProvider;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

// JSON data in: src/test/resources/org/graylog2/indexer/fieldtypes/IndexFieldTypePollerIT.json
public abstract class IndexFieldTypePollerIT extends ElasticsearchBaseTest {

    private final Set<String> allStreamCollection = Set.of("000000000000000000000001", "000000000000000000000002");
    private final String graylog_0 = "graylog_0";
    private final String graylog_1 = "graylog_1";
    private String indexSetId;

    private IndexFieldTypePoller pollerWithoutStreamAwareness;
    private IndexFieldTypePoller pollerWithStreamAwareness;

    private static final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
            .id("index-set-1")
            .title("Index set 1")
            .description("For testing")
            .indexPrefix("graylog")
            .creationDate(ZonedDateTime.now())
            .shards(1)
            .replicas(0)
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
            .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
            .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
            .indexAnalyzer("standard")
            .indexTemplateName("template-1")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .build();
    private TestIndexSet indexSet;

    protected IndicesAdapter createIndicesAdapter() {
        return searchServer().adapters().indicesAdapter();
    }

    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter(final Configuration configuration) {
        return searchServer().adapters().indexFieldTypePollerAdapter(configuration);
    }

    @Before
    public void setUp() throws Exception {
        final Node node = mock(Node.class);
        final Indices indices = new Indices(
                new IndexMappingFactory(node, ImmutableMap.of(
                        MESSAGE_TEMPLATE_TYPE, new MessageIndexTemplateProvider()
                )),
                mock(NodeId.class),
                new NullAuditEventSender(),
                mock(EventBus.class),
                createIndicesAdapter()
        );
        final Configuration withStreamAwarenessOff = spy(new Configuration());
        doReturn(false).when(withStreamAwarenessOff).maintainsStreamBasedFieldLists();
        pollerWithoutStreamAwareness = new IndexFieldTypePoller(indices, new MetricRegistry(), createIndexFieldTypePollerAdapter(withStreamAwarenessOff));
        final Configuration withStreamAwarenessOn = spy(new Configuration());
        doReturn(true).when(withStreamAwarenessOn).maintainsStreamBasedFieldLists();
        pollerWithStreamAwareness = new IndexFieldTypePoller(indices, new MetricRegistry(), createIndexFieldTypePollerAdapter(withStreamAwarenessOn));

        indexSet = new TestIndexSet(indexSetConfig);
        indexSetId = indexSet.getConfig().id();

        importFixture("org/graylog2/indexer/fieldtypes/IndexFieldTypePollerIT.json");
    }

    @Test
    public void streamAwarePoll() {
        final IndexFieldTypesDTO existingFieldTypes = IndexFieldTypesDTO.builder()
                .indexSetId(indexSetId)
                .indexName(graylog_1)
                .hasStreamData(true)
                .build();

        final Set<IndexFieldTypesDTO> dtosNoExisting = pollerWithStreamAwareness.poll(indexSet, Collections.emptySet());
        final Set<IndexFieldTypesDTO> dtos = pollerWithStreamAwareness.poll(indexSet, ImmutableSet.of(existingFieldTypes));

        assertThat(dtosNoExisting).hasSize(2);
        assertThat(dtos).hasSize(1);//graylog_1 tells it has stream data already and is not active write index
        assertThat(dtosNoExisting).containsAll(dtos);

        final List<FieldTypeDTO> expectedFieldDTOs = new ArrayList<>();
        expectedFieldDTOs.addAll(getAlwaysPresentFieldDTOs(allStreamCollection));
        expectedFieldDTOs.addAll(getNeverPresentFieldDTOs());
        expectedFieldDTOs.add(getSourceFieldDTOs(allStreamCollection));
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_only").physicalType("long").streams(Set.of("000000000000000000000002")).build());

        IndexFieldTypesDTO dto = getDtoForIndex(dtosNoExisting, graylog_0);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));

        dto = getDtoForIndex(dtosNoExisting, graylog_1);
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_and_graylog1_only").physicalType("long").streams(Set.of("000000000000000000000002")).build());
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));
    }

    @Test
    public void poll() {
        final IndexFieldTypesDTO existingFieldTypes = IndexFieldTypesDTO.builder()
                .indexSetId(indexSetId)
                .indexName(graylog_1)
                .hasStreamData(true)
                .build();

        final Set<IndexFieldTypesDTO> dtosNoExisting = pollerWithoutStreamAwareness.poll(indexSet, Collections.emptySet());
        final Set<IndexFieldTypesDTO> dtos = pollerWithoutStreamAwareness.poll(indexSet, ImmutableSet.of(existingFieldTypes));

        assertThat(dtosNoExisting).hasSize(2);
        assertThat(dtos).hasSize(1);//graylog_1 tells it has stream data already and is not active write index
        assertThat(dtosNoExisting).containsAll(dtos);

        final List<FieldTypeDTO> expectedFieldDTOs = new ArrayList<>();
        expectedFieldDTOs.addAll(getAlwaysPresentFieldDTOs(Set.of()));
        expectedFieldDTOs.addAll(getNeverPresentFieldDTOs());
        expectedFieldDTOs.add(getSourceFieldDTOs(Set.of()));
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_only").physicalType("long").streams(Set.of()).build());

        IndexFieldTypesDTO dto = getDtoForIndex(dtosNoExisting, graylog_0);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));

        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_and_graylog1_only").physicalType("long").streams(Set.of()).build());
        dto = getDtoForIndex(dtosNoExisting, graylog_1);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));
    }

    @Test
    public void pollIndex() throws Exception {
        IndexFieldTypesDTO dto = pollerWithoutStreamAwareness.pollIndex(graylog_0, indexSetId).orElse(null);
        final List<FieldTypeDTO> expectedFieldDTOs = new ArrayList<>();
        expectedFieldDTOs.addAll(getAlwaysPresentFieldDTOs(Set.of()));
        expectedFieldDTOs.addAll(getNeverPresentFieldDTOs());
        expectedFieldDTOs.add(getSourceFieldDTOs(Set.of()));
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_only").physicalType("long").streams(Set.of()).build());

        verifyDto(dto, graylog_0);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));

        dto = pollerWithoutStreamAwareness.pollIndex(graylog_1, indexSetId).orElse(null);
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_and_graylog1_only").physicalType("long").streams(Set.of()).build());

        verifyDto(dto, graylog_1);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));
    }

    @Test
    public void streamAwarePollIndex() throws Exception {
        IndexFieldTypesDTO dto = pollerWithStreamAwareness.pollIndex(graylog_0, indexSetId).orElse(null);
        final List<FieldTypeDTO> expectedFieldDTOs = new ArrayList<>();
        expectedFieldDTOs.addAll(getAlwaysPresentFieldDTOs(allStreamCollection));
        expectedFieldDTOs.addAll(getNeverPresentFieldDTOs());
        expectedFieldDTOs.add(getSourceFieldDTOs(allStreamCollection));
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_only").physicalType("long").streams(Set.of("000000000000000000000002")).build());

        verifyDto(dto, graylog_0);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));

        dto = pollerWithStreamAwareness.pollIndex(graylog_1, indexSetId).orElse(null);
        expectedFieldDTOs.add(FieldTypeDTO.builder().fieldName("http_stream2_and_graylog1_only").physicalType("long").streams(Set.of("000000000000000000000002")).build());

        verifyDto(dto, graylog_1);
        assertThat(dto.fields()).containsOnly(expectedFieldDTOs.toArray(new FieldTypeDTO[0]));
    }


    private IndexFieldTypesDTO getDtoForIndex(final Set<IndexFieldTypesDTO> dtos, final String indexName) {
        IndexFieldTypesDTO dto = dtos.stream()
                .filter(d -> d.indexName().equals(indexName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No result polling index set " + indexSetId));
        verifyDto(dto, indexName);
        return dto;
    }

    private void verifyDto(final IndexFieldTypesDTO dto, final String expectedIndexName) {
        assertThat(dto).isNotNull();
        assertThat(dto.indexSetId()).isEqualTo(indexSetId);
        assertThat(dto.indexName()).isEqualTo(expectedIndexName);
        assertThat(dto.id()).isNull();
    }

    private List<FieldTypeDTO> getAlwaysPresentFieldDTOs(final Set<String> allStreams) {
        return List.of(
                FieldTypeDTO.builder().fieldName("message").physicalType("text").streams(allStreams).build(),
                FieldTypeDTO.builder().fieldName("http_status").physicalType("keyword").streams(allStreams).build(),
                FieldTypeDTO.builder().fieldName("http_response_time").physicalType("long").streams(allStreams).build(),
                FieldTypeDTO.builder().fieldName("timestamp").physicalType("date").streams(allStreams).build(),
                FieldTypeDTO.builder().fieldName("streams").physicalType("keyword").streams(allStreams).build()
        );
    }

    private FieldTypeDTO getSourceFieldDTOs(final Set<String> allStreams) {
        return FieldTypeDTO.builder()
                .fieldName("source")
                .physicalType("text")
                .properties(Collections.singleton(FieldTypeDTO.Properties.FIELDDATA))
                .streams(allStreams)
                .build();
    }

    private List<FieldTypeDTO> getNeverPresentFieldDTOs() {
        return List.of(
                FieldTypeDTO.builder().fieldName("full_message").physicalType("text").build(),
                FieldTypeDTO.builder().fieldName("gl2_receive_timestamp").physicalType("date").build(),
                FieldTypeDTO.builder().fieldName("gl2_processing_timestamp").physicalType("date").build(),
                FieldTypeDTO.builder().fieldName("gl2_accounted_message_size").physicalType("long").build()
        );
    }
}
