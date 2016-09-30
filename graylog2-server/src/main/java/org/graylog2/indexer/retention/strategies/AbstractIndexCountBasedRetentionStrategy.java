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

import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public abstract class AbstractIndexCountBasedRetentionStrategy implements RetentionStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractIndexCountBasedRetentionStrategy.class);

    private final Indices indices;
    private final ActivityWriter activityWriter;

    public AbstractIndexCountBasedRetentionStrategy(Indices indices,
                                                    ActivityWriter activityWriter) {
        this.indices = requireNonNull(indices);
        this.activityWriter = requireNonNull(activityWriter);
    }

    protected abstract Optional<Integer> getMaxNumberOfIndices();
    protected abstract void retain(String indexName);

    @Override
    public void retain(IndexSet indexSet) {
        final Map<String, Set<String>> deflectorIndices = indexSet.getAllDeflectorAliases();
        final int indexCount = deflectorIndices.size();
        final Optional<Integer> maxIndices = getMaxNumberOfIndices();

        if (!maxIndices.isPresent()) {
            LOG.warn("No retention strategy configuration found, not running index retention!");
            return;
        }

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices.get()) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices.get());
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        final int removeCount = indexCount - maxIndices.get();
        final String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices.get() + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        runRetention(indexSet, deflectorIndices, removeCount);
    }

    private void runRetention(IndexSet indexSet, Map<String, Set<String>> deflectorIndices, int removeCount) {
        for (String indexName : IndexHelper.getOldestIndices(deflectorIndices.keySet(), removeCount)) {
            // Never run against the current deflector target.
            if (deflectorIndices.get(indexName).contains(indexSet.getWriteIndexAlias())) {
                LOG.info("Not running retention against current deflector target <{}>.", indexName);
                continue;
            }

            /*
             * Never run against a re-opened index. Indices are marked as re-opened by storing a setting
             * attribute and we can check for that here.
             */
            if (indices.isReopened(indexName)) {
                LOG.info("Not running retention against reopened index <{}>.", indexName);
                continue;
            }

            final String strategyName = this.getClass().getCanonicalName();
            final String msg = "Running retention strategy [" + strategyName + "] for index <" + indexName + ">";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRetentionThread.class));

            // Sorry if this should ever go mad. Run retention strategy!
            retain(indexName);
        }
    }
}
