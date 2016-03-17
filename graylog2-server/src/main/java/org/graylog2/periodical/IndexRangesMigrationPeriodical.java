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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.ranges.EsIndexRangeService;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.LegacyMongoIndexRangeService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link Periodical} to check if index ranges need to be recalculated and notify the administrators about it.
 *
 * @see <a href="https://github.com/Graylog2/graylog2-server/pull/1274">Refactor index ranges handling (#1274)</a>
 * @since 1.2.0
 */
public class IndexRangesMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRangesMigrationPeriodical.class);

    private final Cluster cluster;
    private final Deflector deflector;
    private final IndexRangeService indexRangeService;
    private final NotificationService notificationService;
    private final LegacyMongoIndexRangeService legacyMongoIndexRangeService;
    private final EsIndexRangeService esIndexRangeService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public IndexRangesMigrationPeriodical(final Cluster cluster,
                                          final Deflector deflector,
                                          final IndexRangeService indexRangeService,
                                          final NotificationService notificationService,
                                          final LegacyMongoIndexRangeService legacyMongoIndexRangeService,
                                          final EsIndexRangeService esIndexRangeService,
                                          final ClusterConfigService clusterConfigService) {
        this.cluster = checkNotNull(cluster);
        this.deflector = checkNotNull(deflector);
        this.indexRangeService = checkNotNull(indexRangeService);
        this.notificationService = checkNotNull(notificationService);
        this.legacyMongoIndexRangeService = checkNotNull(legacyMongoIndexRangeService);
        this.esIndexRangeService = checkNotNull(esIndexRangeService);
        this.clusterConfigService = checkNotNull(clusterConfigService);
    }

    @Override
    public void doRun() {
        final Optional<MongoIndexRangesMigrationComplete> migrationComplete =
                clusterConfigService.get(MongoIndexRangesMigrationComplete.class);
        if (migrationComplete.isPresent() && migrationComplete.get().complete) {
            LOG.debug("Migration of index ranges (pre Graylog 1.2.2) already complete. Skipping migration process.");
            return;
        }

        while (!cluster.isConnected() || !cluster.isHealthy()) {
            Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
        }

        final Set<String> indexNames = ImmutableSet.copyOf(deflector.getAllDeflectorIndexNames());

        // Migrate old MongoDB index ranges
        final SortedSet<IndexRange> mongoIndexRanges = legacyMongoIndexRangeService.findAll();
        for (IndexRange indexRange : mongoIndexRanges) {
            if (indexNames.contains(indexRange.indexName())) {
                LOG.info("Migrating index range from MongoDB: {}", indexRange);
                indexRangeService.save(indexRange);
            } else {
                LOG.info("Removing stale index range from MongoDB: {}", indexRange);
            }

            legacyMongoIndexRangeService.delete(indexRange.indexName());
        }

        // Migrate old Elasticsearch index ranges
        final SortedSet<IndexRange> esIndexRanges = esIndexRangeService.findAll();
        for (IndexRange indexRange : esIndexRanges) {
            LOG.info("Migrating index range from Elasticsearch: {}", indexRange);
            indexRangeService.save(indexRange);
        }

        // Check whether all index ranges have been migrated
        final int numberOfIndices = indexNames.size();
        final int numberOfIndexRanges = indexRangeService.findAll().size();
        if (numberOfIndices > numberOfIndexRanges) {
            LOG.info("There are more indices ({}) than there are index ranges ({}). Notifying administrator.",
                    numberOfIndices, numberOfIndexRanges);
            final Notification notification = notificationService.buildNow()
                    .addSeverity(Notification.Severity.URGENT)
                    .addType(Notification.Type.INDEX_RANGES_RECALCULATION)
                    .addDetail("indices", numberOfIndices)
                    .addDetail("index_ranges", numberOfIndexRanges);
            notificationService.publishIfFirst(notification);
        }

        clusterConfigService.write(new MongoIndexRangesMigrationComplete(true));
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @JsonAutoDetect
    public static class MongoIndexRangesMigrationComplete {
        @JsonProperty
        public boolean complete;

        @JsonCreator
        public MongoIndexRangesMigrationComplete(@JsonProperty("complete") boolean complete) {
            this.complete = complete;
        }
    }
}
