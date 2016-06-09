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
package org.graylog2.indexer.retention.strategies;

import com.google.common.base.Stopwatch;
import org.graylog2.auditlog.AuditLogger;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ClosingRetentionStrategy extends AbstractIndexCountBasedRetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(ClosingRetentionStrategy.class);

    private final Indices indices;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public ClosingRetentionStrategy(Deflector deflector,
                                    Indices indices,
                                    ActivityWriter activityWriter,
                                    ClusterConfigService clusterConfigService,
                                    AuditLogger auditLogger) {
        super(deflector, indices, activityWriter, auditLogger);
        this.indices = indices;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    protected Optional<Integer> getMaxNumberOfIndices() {
        final ClosingRetentionStrategyConfig config = clusterConfigService.get(ClosingRetentionStrategyConfig.class);

        if (config != null) {
            return Optional.of(config.maxNumberOfIndices());
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void retain(String indexName) {
        final Stopwatch sw = Stopwatch.createStarted();

        indices.close(indexName);

        LOG.info("Finished index retention strategy [close] for index <{}> in {}ms.", indexName,
                sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public Class<? extends RetentionStrategyConfig> configurationClass() {
        return ClosingRetentionStrategyConfig.class;
    }

    @Override
    public RetentionStrategyConfig defaultConfiguration() {
        return ClosingRetentionStrategyConfig.createDefault();
    }
}
