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

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class NoopRetentionStrategy extends AbstractIndexCountBasedRetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(NoopRetentionStrategy.class);

    @Inject
    public NoopRetentionStrategy(Indices indices, ActivityWriter activityWriter) {
        super(indices, activityWriter);
    }

    @Override
    protected Optional<Integer> getMaxNumberOfIndices(IndexSet indexSet) {
        return Optional.of(Integer.MAX_VALUE);
    }

    @Override
    protected void retain(String indexName, IndexSet indexSet) {
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
