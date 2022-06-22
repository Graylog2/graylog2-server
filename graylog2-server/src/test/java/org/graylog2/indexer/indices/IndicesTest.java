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
package org.graylog2.indexer.indices;

import com.google.common.eventbus.EventBus;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.IgnoreIndexTemplate;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.IndexTemplateNotFoundException;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.blocks.IndicesBlockStatus;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndicesTest {

    private Indices underTest;

    @Mock
    private IndexMappingFactory indexMappingFactory;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private EventBus eventBus;

    @Mock
    private IndicesAdapter indicesAdapter;

    @BeforeEach
    public void setup() {
        underTest = new Indices(
                indexMappingFactory,
                nodeId,
                auditEventSender,
                eventBus,
                indicesAdapter
        );
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateExistsOnIgnoreIndexTemplate_thenNoExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(true,
                        "Reasom", "test", "test-template", null));

        when(indicesAdapter.indexTemplateExists("test-template")).thenReturn(true);

        assertThatCode(() -> underTest.ensureIndexTemplate(
                indexSetConfig("test", "test-template", "custom")))
                .doesNotThrowAnyException();
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateDoesntExistOnIgnoreIndexTemplateAndFailOnMissingTemplateIsTrue_thenExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(true,
                        "Reasom", "test", "test-template", null));

        when(indicesAdapter.indexTemplateExists("test-template")).thenReturn(false);

        assertThatCode(() -> underTest.ensureIndexTemplate(indexSetConfig("test",
                "test-template", "custom")))
                .isExactlyInstanceOf(IndexTemplateNotFoundException.class)
                .hasMessage("No index template with name 'test-template' (type - 'custom') found in Elasticsearch");
    }

    @Test
    public void ensureIndexTemplate_IfIndexTemplateDoesntExistOnIgnoreIndexTemplateAndFailOnMissingTemplateIsFalse_thenNoExceptionThrown() {
        when(indexMappingFactory.createIndexMapping(any()))
                .thenThrow(new IgnoreIndexTemplate(false,
                        "Reasom", "test", "test-template", null));

        assertThatCode(() -> underTest.ensureIndexTemplate(indexSetConfig("test",
                "test-template", "custom")))
                .doesNotThrowAnyException();
    }

    @Test
    public void testGetIndicesBlocksStatusReturnsNoBlocksOnNullIndicesList() {
        final IndicesBlockStatus indicesBlocksStatus = underTest.getIndicesBlocksStatus(null);
        assertNotNull(indicesBlocksStatus);
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());
    }

    @Test
    public void testGetIndicesBlocksStatusReturnsNoBlocksOnEmptyIndicesList() {
        final IndicesBlockStatus indicesBlocksStatus = underTest.getIndicesBlocksStatus(Collections.emptyList());
        assertNotNull(indicesBlocksStatus);
        assertEquals(0, indicesBlocksStatus.countBlockedIndices());
    }

    private TestIndexSet indexSetConfig(String indexPrefix, String indexTemplaNameName, String indexTemplateType) {
        return new TestIndexSet(IndexSetConfig.builder()
                .id("index-set-1")
                .title("Index set 1")
                .description("For testing")
                .indexPrefix(indexPrefix)
                .creationDate(ZonedDateTime.now())
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .indexAnalyzer("standard")
                .indexTemplateName(indexTemplaNameName)
                .indexTemplateType(indexTemplateType)
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build());
    }
}
