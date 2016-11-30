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
package org.graylog2.migrations;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class V20161130141500_DefaultStreamRecalcIndexRanges extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161130141500_DefaultStreamRecalcIndexRanges.class);

    private final IndexSetRegistry indexSetRegistry;
    private final IndexRangeService indexRangeService;
    private final CreateNewSingleIndexRangeJob.Factory rebuildIndexRangeJobFactory;
    private final Cluster cluster;

    @Inject
    public V20161130141500_DefaultStreamRecalcIndexRanges(final IndexSetRegistry indexSetRegistry,
                                                          final IndexRangeService indexRangeService,
                                                          final CreateNewSingleIndexRangeJob.Factory rebuildIndexRangeJobFactory,
                                                          final Cluster cluster) {
        this.indexSetRegistry = indexSetRegistry;
        this.indexRangeService = indexRangeService;
        this.rebuildIndexRangeJobFactory = rebuildIndexRangeJobFactory;
        this.cluster = cluster;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-30T14:15:00Z");
    }

    @Override
    public void upgrade() {
        final Optional<IndexSet> optDefaultIndexSet = indexSetRegistry.getAllIndexSets().stream()
                .filter(indexSet -> indexSet.getConfig().isDefault())
                .findFirst();
        if (!optDefaultIndexSet.isPresent()) {
            LOG.error("No default index set found, this should not happen. Unable to assign streams to older indices");
            return;
        }
        final IndexSet defaultIndexSet = optDefaultIndexSet.get();

        if (!cluster.isConnected()) {
            LOG.info("Cluster not connected yet, delaying migration until it is reachable.");
            while (true) {
                try {
                    cluster.waitForConnectedAndDeflectorHealthy();
                    break;
                } catch (InterruptedException | TimeoutException e) {
                    LOG.warn("Interrupted or timed out waiting for Elasticsearch cluster, checking again.");
                }
            }
        }
        final Set<String> indexRangesWithoutStreams = indexRangeService.findAll().stream()
                .filter(indexRange -> defaultIndexSet.isManagedIndex(indexRange.indexName()))
                .filter(indexRange -> indexRange.streamIds() == null)
                .map(IndexRange::indexName)
                .collect(Collectors.toSet());

        if (indexRangesWithoutStreams.size() == 0) {
            // all ranges have a stream list, even if it is empty, nothing more to do
            return;
        }

        final String currentWriteTarget;
        try {
            currentWriteTarget = defaultIndexSet.getCurrentActualTargetIndex();
        } catch (TooManyAliasesException e) {
            LOG.error("Multiple write targets found for write alias. Cannot continue to assign streams to older indices", e);
            return;
        }
        for (String indexName : defaultIndexSet.getManagedIndicesNames()) {
            if (indexName.equals(currentWriteTarget)) {
                // do not recalculate for current write target
                continue;
            }
            if (!indexRangesWithoutStreams.contains(indexName)) {
                // already computed streams for this index
                continue;
            }
            LOG.info("Recalculating streams in index {}", indexName);
            final CreateNewSingleIndexRangeJob createNewSingleIndexRangeJob = rebuildIndexRangeJobFactory.create(indexSetRegistry, indexName);
            createNewSingleIndexRangeJob.execute();
        }

    }
}
