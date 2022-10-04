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
import static org.mockito.Mockito.mock;

// JSON data in: src/test/resources/org/graylog2/indexer/fieldtypes/IndexFieldTypePollerIT.json
public abstract class IndexFieldTypePollerIT extends ElasticsearchBaseTest {
    private static final String INDEX_NAME = "graylog_0";
    private final Set<String> expectedStreamCollection = Set.of("000000000000000000000001");

    private IndexFieldTypePoller poller;

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

    protected IndexFieldTypePollerAdapter createIndexFieldTypePollerAdapter() {
        return searchServer().adapters().indexFieldTypePollerAdapter();
    }

    @Before
    public void setUp() throws Exception {
        final Node node = mock(Node.class);
        @SuppressWarnings("UnstableApiUsage")
        final Indices indices = new Indices(
                new IndexMappingFactory(node, ImmutableMap.of(
                        MESSAGE_TEMPLATE_TYPE, new MessageIndexTemplateProvider()
                )),
                mock(NodeId.class),
                new NullAuditEventSender(),
                mock(EventBus.class),
                createIndicesAdapter()
        );
        poller = new IndexFieldTypePoller(indices, new MetricRegistry(), createIndexFieldTypePollerAdapter());
        indexSet = new TestIndexSet(indexSetConfig);

        importFixture("org/graylog2/indexer/fieldtypes/IndexFieldTypePollerIT.json");
    }

    @Test
    public void poll() {
        final String indexSetId = indexSet.getConfig().id();
        final IndexFieldTypesDTO existingFieldTypes = IndexFieldTypesDTO.builder()
                .indexSetId(indexSetId)
                .indexName("graylog_1")
                .build();

        final Set<IndexFieldTypesDTO> dtosNoExisting = poller.poll(indexSet, Collections.emptySet());
        final Set<IndexFieldTypesDTO> dtos = poller.poll(indexSet, ImmutableSet.of(existingFieldTypes));

        assertThat(dtosNoExisting).hasSize(2);
        assertThat(dtos).hasSize(2);

        IndexFieldTypesDTO dto = dtos.stream()
                .filter(d -> d.indexName().equals("graylog_0"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No result polling index set " + indexSetId));
        assertThat(dto.indexSetId()).isEqualTo(indexSetId);
        assertThat(dto.indexName()).isEqualTo("graylog_0");
        assertThat(dto.id()).isNull();
        //TODO: Add steam/index specific fields
        assertThat(dto.fields()).containsOnly(getCommonFieldDTOs().toArray(new FieldTypeDTO[0]));

        dto = dtos.stream()
                .filter(d -> d.indexName().equals("graylog_1"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No result polling index set " + indexSetId));
        assertThat(dto.indexSetId()).isEqualTo(indexSetId);
        assertThat(dto.indexName()).isEqualTo("graylog_1");
        assertThat(dto.id()).isNull();
        //TODO: Add steam/index specific fields
        assertThat(dto.fields()).containsOnly(getCommonFieldDTOs().toArray(new FieldTypeDTO[0]));
    }

    @Test
    public void pollIndex() throws Exception {
        final String indexSetId = indexSet.getConfig().id();

        IndexFieldTypesDTO dto = poller.pollIndex("graylog_0", indexSetId).orElse(null);

        assertThat(dto).isNotNull();
        assertThat(dto.indexSetId()).isEqualTo(indexSetId);
        assertThat(dto.indexName()).isEqualTo("graylog_0");
        assertThat(dto.id()).isNull();
        //TODO: Add steam/index specific fields
        assertThat(dto.fields()).containsOnly(getCommonFieldDTOs().toArray(new FieldTypeDTO[0]));

        dto = poller.pollIndex("graylog_1", indexSetId).orElse(null);

        assertThat(dto).isNotNull();
        assertThat(dto.indexSetId()).isEqualTo(indexSetId);
        assertThat(dto.indexName()).isEqualTo("graylog_1");
        assertThat(dto.id()).isNull();
        //TODO: Add steam/index specific fields
        assertThat(dto.fields()).containsOnly(getCommonFieldDTOs().toArray(new FieldTypeDTO[0]));
    }

    private List<FieldTypeDTO> getCommonFieldDTOs() {
        return new ArrayList<>(List.of(
                FieldTypeDTO.builder().fieldName("message").physicalType("text").streams(expectedStreamCollection).build(),
                FieldTypeDTO.builder().fieldName("full_message").physicalType("text").build(),
                FieldTypeDTO.builder().fieldName("source").physicalType("text").properties(Collections.singleton(FieldTypeDTO.Properties.FIELDDATA)).streams(expectedStreamCollection).build(),
                FieldTypeDTO.builder().fieldName("http_status").physicalType("keyword").streams(expectedStreamCollection).build(),
                FieldTypeDTO.builder().fieldName("http_response_time").physicalType("long").streams(expectedStreamCollection).build(),
                FieldTypeDTO.builder().fieldName("timestamp").physicalType("date").streams(expectedStreamCollection).build(),
                FieldTypeDTO.builder().fieldName("gl2_receive_timestamp").physicalType("date").build(),
                FieldTypeDTO.builder().fieldName("gl2_processing_timestamp").physicalType("date").build(),
                FieldTypeDTO.builder().fieldName("gl2_accounted_message_size").physicalType("long").build(),
                FieldTypeDTO.builder().fieldName("streams").physicalType("keyword").streams(expectedStreamCollection).build()
        ));
    }
}
