/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.fieldtypes;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchConfiguration;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import org.graylog2.ElasticsearchBase;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.system.processing.InMemoryProcessingStatusRecorder;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// JSON data in: src/test/resources/org/graylog2/indexer/fieldtypes/IndexFieldTypePollerIT.json
public class IndexFieldTypePollerIT extends ElasticsearchBase {
    private static final String INDEX_NAME = "graylog_0";

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
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

    @Override
    protected ElasticsearchConfiguration.Builder elasticsearchConfiguration() {
        final Map<String, Map<String, Object>> messageTemplates = Collections.singletonMap("graylog-test-internal", indexMapping().messageTemplate("*", "standard"));
        return super.elasticsearchConfiguration()
                .indexTemplates(messageTemplates)
                .deleteAllIndices(true);
    }

    @Before
    public void setUp() throws Exception {
        final Indices indices = new Indices(client(),
                new ObjectMapperProvider().get(),
                new IndexMappingFactory(new Node(client())),
                new Messages(new MetricRegistry(), client(), new InMemoryProcessingStatusRecorder(), true),
                mock(NodeId.class),
                new NullAuditEventSender(),
                new EventBus("index-field-type-poller-it"));
        poller = new IndexFieldTypePoller(client(), indices, new MetricRegistry());
        indexSet = new TestIndexSet(indexSetConfig);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void poll() throws Exception {
        final String indexSetId = indexSet.getConfig().id();
        final IndexFieldTypesDTO existingFieldTypes = IndexFieldTypesDTO.builder()
                .indexSetId(indexSetId)
                .indexName("graylog_1")
                .build();

        final Set<IndexFieldTypesDTO> dtosNoExisting = poller.poll(indexSet, Collections.emptySet());
        final Set<IndexFieldTypesDTO> dtos = poller.poll(indexSet, ImmutableSet.of(existingFieldTypes));

        final IndexFieldTypesDTO dto = dtos.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No result polling index set " + indexSetId));

        try {
            assertThat(dtosNoExisting).hasSize(2);
            assertThat(dtos).hasSize(1);

            assertThat(dto.indexSetId()).isEqualTo(indexSetId);
            assertThat(dto.indexName()).isEqualTo(INDEX_NAME);
            assertThat(dto.id()).isNull();
            assertThat(dto.fields()).containsOnly(
                    FieldTypeDTO.create("message", "text"),
                    FieldTypeDTO.create("full_message", "text"),
                    FieldTypeDTO.create("source", "text"),
                    FieldTypeDTO.create("http_status", "keyword"),
                    FieldTypeDTO.create("http_response_time", "long"),
                    FieldTypeDTO.create("timestamp", "date"),
                    FieldTypeDTO.create("gl2_receive_timestamp", "date"),
                    FieldTypeDTO.create("gl2_processing_timestamp", "date"),
                    FieldTypeDTO.create("streams", "keyword")
            );
        } finally {
            deleteIndex(INDEX_NAME);
            deleteIndex("graylog_1");
        }
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void pollIndex() throws Exception {
        final String indexSetId = indexSet.getConfig().id();

        final IndexFieldTypesDTO dto = poller.pollIndex("graylog_0", indexSetId).orElse(null);

        try {
            assertThat(dto).isNotNull();
            assertThat(dto.indexSetId()).isEqualTo(indexSetId);
            assertThat(dto.indexName()).isEqualTo(INDEX_NAME);
            assertThat(dto.id()).isNull();
            assertThat(dto.fields()).containsOnly(
                    FieldTypeDTO.create("message", "text"),
                    FieldTypeDTO.create("full_message", "text"),
                    FieldTypeDTO.create("source", "text"),
                    FieldTypeDTO.create("http_status", "keyword"),
                    FieldTypeDTO.create("http_response_time", "long"),
                    FieldTypeDTO.create("timestamp", "date"),
                    FieldTypeDTO.create("gl2_receive_timestamp", "date"),
                    FieldTypeDTO.create("gl2_processing_timestamp", "date"),
                    FieldTypeDTO.create("streams", "keyword")
            );
        } finally {
            deleteIndex(INDEX_NAME);
            deleteIndex("graylog_1");
        }
    }
}
