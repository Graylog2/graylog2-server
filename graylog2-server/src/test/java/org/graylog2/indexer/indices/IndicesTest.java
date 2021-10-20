package org.graylog2.indexer.indices;

import com.google.common.eventbus.EventBus;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.IgnoreIndexTemplate;
import org.graylog2.indexer.IndexMappingFactory;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
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
                .isExactlyInstanceOf(ElasticsearchException.class)
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
