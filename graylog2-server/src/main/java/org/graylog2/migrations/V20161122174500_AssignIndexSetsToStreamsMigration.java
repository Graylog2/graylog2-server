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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;

public class V20161122174500_AssignIndexSetsToStreamsMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161122174500_AssignIndexSetsToStreamsMigration.class);

    private final StreamService streamService;
    private final IndexSetService indexSetService;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public V20161122174500_AssignIndexSetsToStreamsMigration(final StreamService streamService,
                                                             final IndexSetService indexSetService,
                                                             final ClusterConfigService clusterConfigService,
                                                             final ClusterEventBus clusterEventBus) {
        this.streamService = streamService;
        this.indexSetService = indexSetService;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-11-22T17:45:00Z");
    }

    @Override
    public void upgrade() {
        // Only run this migration once.
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final IndexSetConfig indexSetConfig = findDefaultIndexSet();
        final ImmutableSet.Builder<String> completedStreamIds = ImmutableSet.builder();
        final ImmutableSet.Builder<String> failedStreamIds = ImmutableSet.builder();

        // Assign the "default index set" to all existing streams. Until now, there was no way to manually create
        // index sets, so the only one that exists is the "default" one created by an earlier migration.
        for (Stream stream : streamService.loadAll()) {
            if (isNullOrEmpty(stream.getIndexSetId())) {
                LOG.info("Assigning index set <{}> ({}) to stream <{}> ({})", indexSetConfig.id(),
                        indexSetConfig.title(), stream.getId(), stream.getTitle());
                stream.setIndexSetId(indexSetConfig.id());
                try {
                    streamService.save(stream);
                    completedStreamIds.add(stream.getId());
                } catch (ValidationException e) {
                    LOG.error("Unable to save stream <{}>", stream.getId(), e);
                    failedStreamIds.add(stream.getId());
                }
            }
        }

        // Mark this migration as done.
        clusterConfigService.write(MigrationCompleted.create(indexSetConfig.id(), completedStreamIds.build(), failedStreamIds.build()));

        // Make sure the stream router will reload the changed streams.
        clusterEventBus.post(StreamsChangedEvent.create(completedStreamIds.build()));
    }

    private IndexSetConfig findDefaultIndexSet() {
        final List<IndexSetConfig> indexSetConfigs = indexSetService.findAll();

        // If there is more than one index set, we have a problem. Since there wasn't a way to create index sets
        // manually until now, this should not happen.
        checkState(indexSetConfigs.size() < 2, "Found more than one index set config!");

        // If there is no index set, a previous migration didn't work.
        return indexSetConfigs.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Couldn't find any index set config!"));
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("index_set_id")
        public abstract String indexSetId();

        @JsonProperty("completed_stream_ids")
        public abstract Set<String> completedStreamIds();

        @JsonProperty("failed_stream_ids")
        public abstract Set<String> failedStreamIds();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("index_set_id") String indexSetId,
                                                @JsonProperty("completed_stream_ids") Set<String> completedStreamIds,
                                                @JsonProperty("failed_stream_ids") Set<String> failedStreamIds) {
            return new AutoValue_V20161122174500_AssignIndexSetsToStreamsMigration_MigrationCompleted(indexSetId, completedStreamIds, failedStreamIds);
        }
    }
}
