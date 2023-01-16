package org.graylog2.indexer.rotation.strategies;

import org.graylog2.audit.AuditEventSender;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Period;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.rotation.strategies.SmartRotationStrategy.MAX_INDEX_SIZE;
import static org.graylog2.indexer.rotation.strategies.SmartRotationStrategy.MIN_INDEX_SIZE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartRotationStrategyTest {

    private SmartRotationStrategy smartRotationStrategy;

    @Mock
    private Indices indices;

    @Mock
    private NodeId nodeId;

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private IndexSet indexSet;

    private SmartRotationStrategyConfig smartRotationStrategyConfig;
    private DateTime now;

    @BeforeEach
    void setUp() {
        smartRotationStrategy = new SmartRotationStrategy(indices, nodeId, auditEventSender, new ElasticsearchConfiguration());

        smartRotationStrategyConfig = SmartRotationStrategyConfig.builder().build();
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.rotationStrategy()).thenReturn(smartRotationStrategyConfig);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        now = DateTime.now(DateTimeZone.UTC);
    }

    @Test
    void shouldRotateWhenTooBig() {
        final DateTime creationDate = now.minus(Duration.standardMinutes(10));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MAX_INDEX_SIZE.toBytes() + 10));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
    }

    @Test
    void shouldRotateWhenRightSizedAndOverRotationPeriod() {
        final DateTime creationDate = now.minus(Duration.standardDays(1));
        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(MIN_INDEX_SIZE.toBytes() + 10));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
    }

    @Test
    void shouldRotateWhenBeyondLeeWay() {
        final Period leeWay = smartRotationStrategyConfig.indexLifetimeHard().minus(smartRotationStrategyConfig.indexLifetimeSoft());
        final java.time.Duration leeWayDuration = java.time.Duration.ofDays(leeWay.getDays());
        final DateTime creationDate = now.minus(leeWayDuration.toMillis());

        when(indices.indexCreationDate("index_0")).thenReturn(Optional.of(creationDate));
        when(indices.getStoreSizeInBytes("index_0")).thenReturn(Optional.of(5L));

        final SmartRotationStrategy.Result result = smartRotationStrategy.shouldRotate("index_0", indexSet);

        assertThat(result.shouldRotate()).isEqualTo(true);
    }
}
