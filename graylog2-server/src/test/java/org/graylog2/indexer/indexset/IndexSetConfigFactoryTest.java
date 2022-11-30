package org.graylog2.indexer.indexset;

import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class IndexSetConfigFactoryTest {
    public static final IndexSetConfig TEST_INDEX_SET_CONFIG = IndexSetConfig.builder()
            .isWritable(false)
            .title("Title")
            .description("Description.")
            .indexPrefix("prefix")
            // Use a special match pattern and wildcard to match restored indices like `restored-archive-graylog_33`
            .indexMatchPattern("test*")
            .indexWildcard("test*")
            .rotationStrategy(MessageCountRotationStrategyConfig.create(Integer.MAX_VALUE))
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .retentionStrategy(NoopRetentionStrategyConfig.create(Integer.MAX_VALUE))
            .retentionStrategyClass(NoopRetentionStrategy.class.getCanonicalName())
            .shards(4)
            .replicas(0)
            .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
            .indexTemplateName("template")
            .indexAnalyzer("standard")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .isRegular(false)
            .build();

}
