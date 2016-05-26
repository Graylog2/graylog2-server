package org.graylog2.indexer.retention.strategies;

import com.google.common.base.Optional;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class NoopRetentionStrategy extends AbstractIndexCountBasedRetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(NoopRetentionStrategy.class);

    @Inject
    public NoopRetentionStrategy(Deflector deflector, Indices indices, ActivityWriter activityWriter) {
        super(deflector, indices, activityWriter);
    }

    @Override
    protected Optional<Integer> getMaxNumberOfIndices() {
        return Optional.of(Integer.MAX_VALUE);
    }

    @Override
    protected void retain(String indexName) {
        LOG.info("Not running any index retention. This is the no-op index rotation strategy.");
    }

    @Override
    public Class<? extends RetentionStrategyConfig> configurationClass() {
        return NoopRetentionStrategyConfig.class;
    }

    @Override
    public RetentionStrategyConfig defaultConfiguration() {
        return NoopRetentionStrategyConfig.createDefault();
    }
}
