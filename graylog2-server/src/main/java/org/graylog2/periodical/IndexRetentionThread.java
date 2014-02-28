/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.periodical;

import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.indexer.IndexHelper;
import org.graylog2.indexer.NoTargetIndexException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.indexer.retention.RetentionStrategyFactory;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class IndexRetentionThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRetentionThread.class);

    @Override
    public void run() {
        Map<String, IndexStats> indices = core.getDeflector().getAllDeflectorIndices();
        int indexCount = indices.size();
        int maxIndices = core.getConfiguration().getMaxNumberOfIndices();

        // Do we have more indices than the configured maximum?
        if (indexCount <= maxIndices) {
            LOG.debug("Number of indices ({}) lower than limit ({}). Not performing any retention actions.",
                    indexCount, maxIndices);
            return;
        }

        // We have more indices than the configured maximum! Remove as many as needed.
        int removeCount = indexCount-maxIndices;
        String msg = "Number of indices (" + indexCount + ") higher than limit (" + maxIndices + "). " +
                "Running retention for " + removeCount + " indices.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, IndexRetentionThread.class));

        try {
            runRetention(
                    RetentionStrategyFactory.fromString(core, core.getConfiguration().getRetentionStrategy()),
                    indices,
                    removeCount
            );
        } catch (RetentionStrategyFactory.NoSuchStrategyException e) {
            LOG.error("Could not run index retention. No such strategy.", e);
        } catch (NoTargetIndexException e) {
            LOG.error("Could not run index retention. No target index.", e);
        }
    }

    public void runRetention(RetentionStrategy strategy, Map<String, IndexStats> indices, int removeCount) throws NoTargetIndexException {
        for (String indexName : IndexHelper.getOldestIndices(indices.keySet(), removeCount)) {
            // Never run against the current deflector target.
            if (core.getDeflector().getCurrentActualTargetIndex().equals(indexName)) {
                LOG.info("Not running retention against current deflector target <{}>.", indexName);
                continue;
            }

            /*
             * Never run against a re-opened index. Indices are marked as re-opened by storing a setting
             * attribute and we can check for that here.
             */
            if (core.getIndexer().indices().isReopened(indexName)) {
                LOG.info("Not running retention against reopened index <{}>.", indexName);
                continue;
            }

            String msg = "Running retention strategy [" + strategy.getClass().getCanonicalName() + "] " +
                    "for index <" + indexName + ">";
            LOG.info(msg);
            core.getActivityWriter().write(new Activity(msg, IndexRetentionThread.class));

            // Sorry if this should ever go mad. Run retention strategy!
            strategy.runStrategy(indexName);
        }

        // Re-calculate index ranges.
        try {
            core.getSystemJobManager().submit(new RebuildIndexRangesJob(core));
        } catch (SystemJobConcurrencyException e) {
            String msg = "Could not re-calculate index ranges after running retention: Maximum concurrency of job is reached.";
            core.getActivityWriter().write(new Activity(msg, IndexRetentionThread.class));
            LOG.error(msg);
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
        return core.getConfiguration().performRetention();
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
        // Five minutes.
        return 300;
    }

}