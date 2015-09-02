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
package org.graylog2.periodical;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.retention.RetentionStrategyFactory;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;

public class IndexRetentionThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRetentionThread.class);

    private final ElasticsearchConfiguration configuration;
    private final Deflector deflector;
    private final Cluster cluster;
    private final ActivityWriter activityWriter;
    private final Indices indices;

    @Inject
    public IndexRetentionThread(ElasticsearchConfiguration configuration,
                                Deflector deflector,
                                Indices indices,
                                Cluster cluster,
                                ActivityWriter activityWriter) {
        this.configuration = configuration;
        this.deflector = deflector;
        this.indices = indices;
        this.cluster = cluster;
        this.activityWriter = activityWriter;
    }

    @Override
    public void doRun() {
        if (!cluster.isConnected() || !cluster.isHealthy()) {
            LOG.info("Elasticsearch cluster not available, skipping index retention checks.");
            return;
        }

        final Map<String, IndexStats> deflectorIndices = deflector.getAllDeflectorIndices();
        final int indexCount = deflectorIndices.size();
        final int maxIndices = configuration.getMaxNumberOfIndices();

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices);
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        final int removeCount = indexCount - maxIndices;
        final String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, IndexRetentionThread.class));

        try {
            runRetention(
                    RetentionStrategyFactory.fromString(configuration.getRetentionStrategy(), indices),
                    deflectorIndices,
                    removeCount
            );
        } catch (RetentionStrategyFactory.NoSuchStrategyException e) {
            LOG.error("Could not run index retention. No such strategy.", e);
        } catch (NoTargetIndexException e) {
            LOG.error("Could not run index retention. No target index.", e);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    public void runRetention(RetentionStrategy strategy, Map<String, IndexStats> deflectorIndices, int removeCount) throws NoTargetIndexException {
        for (String indexName : IndexHelper.getOldestIndices(deflectorIndices.keySet(), removeCount)) {
            // Never run against the current deflector target.
            if (indexName.equals(deflector.getCurrentActualTargetIndex())) {
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

            final String msg = "Running retention strategy [" + strategy.getClass().getCanonicalName() + "] " +
                    "for index <" + indexName + ">";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, IndexRetentionThread.class));

            // Sorry if this should ever go mad. Run retention strategy!
            strategy.runStrategy(indexName);
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return configuration.performRetention();
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return (int) MINUTES.toSeconds(5);
    }
}