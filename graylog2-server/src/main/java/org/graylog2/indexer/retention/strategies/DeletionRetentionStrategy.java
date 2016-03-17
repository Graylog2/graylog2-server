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

public class DeletionRetentionStrategy extends AbstractIndexCountBasedRetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(DeletionRetentionStrategy.class);

    private final Indices indices;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public DeletionRetentionStrategy(Deflector deflector,
                                     Indices indices,
                                     ActivityWriter activityWriter,
                                     ClusterConfigService clusterConfigService) {
        super(deflector, indices, activityWriter);
        this.indices = indices;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    protected Optional<Integer> getMaxNumberOfIndices() {
        return clusterConfigService.get(DeletionRetentionStrategyConfig.class)
                .flatMap(config -> Optional.of(config.maxNumberOfIndices()));
    }

    @Override
    public void retain(String indexName) {
        final Stopwatch sw = Stopwatch.createStarted();

        indices.delete(indexName);

        LOG.info("Finished index retention strategy [delete] for index <{}> in {}ms.", indexName,
                sw.stop().elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public Class<? extends RetentionStrategyConfig> configurationClass() {
        return DeletionRetentionStrategyConfig.class;
    }

    @Override
    public RetentionStrategyConfig defaultConfiguration() {
        return DeletionRetentionStrategyConfig.defaultConfig();
    }
}
