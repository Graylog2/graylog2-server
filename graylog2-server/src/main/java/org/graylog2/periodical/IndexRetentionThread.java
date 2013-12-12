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
import org.graylog2.indexer.retention.RetentionStrategyFactory;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.system.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.Core;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class IndexRetentionThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRetentionThread.class);

    private final Core server;
    
    public static final int INITIAL_DELAY = 0;
    public static final int PERIOD = 300; // Run every five minutes.

    public IndexRetentionThread(Core server) {
        this.server = server;
    }

    @Override
    public void run() {
        Map<String, IndexStats> indices = server.getDeflector().getAllDeflectorIndices();
        int indexCount = indices.size();
        int maxIndices = server.getConfiguration().getMaxNumberOfIndices();

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
        server.getActivityWriter().write(new Activity(msg, IndexRetentionThread.class));

        try {
            runRetention(
                    RetentionStrategyFactory.fromString(server, server.getConfiguration().getRetentionStrategy()),
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
            if (server.getDeflector().getCurrentActualTargetIndex().equals(indexName)) {
                LOG.info("Not running retention against current deflector target <{}>.", indexName);
                continue;
            }

            /*
             * Never run against a re-opened index. Indices are marked as re-opened by storing a setting
             * attribute and we can check for that here.
             */
            if (server.getIndexer().indices().isReopened(indexName)) {
                LOG.info("Not running retention against reopened index <{}>.", indexName);
                continue;
            }

            String msg = "Running retention strategy [" + strategy.getClass().getCanonicalName() + "] " +
                    "for index <" + indexName + ">";
            LOG.info(msg);
            server.getActivityWriter().write(new Activity(msg, IndexRetentionThread.class));

            // Sorry if this should ever go mad. Run retention strategy!
            strategy.runStrategy(indexName);

            // Remove index from ranges.
            IndexRange.destroy(server, indexName);
        }
    }

}