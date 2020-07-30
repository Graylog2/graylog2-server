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
package org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Set;

public class V20200730000000_AddGl2MessageIdFieldAliasForEvents extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20200730000000_AddGl2MessageIdFieldAliasForEvents.class);

    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchAdapter elasticsearch;
    private final ElasticsearchConfiguration elasticsearchConfig;

    public V20200730000000_AddGl2MessageIdFieldAliasForEvents(
            ClusterConfigService clusterConfigService,
            ElasticsearchAdapter elasticsearch,
            ElasticsearchConfiguration elasticsearchConfig) {
        this.clusterConfigService = clusterConfigService;
        this.elasticsearch = elasticsearch;
        this.elasticsearchConfig = elasticsearchConfig;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-07-30T00:00:00Z");
    }

    @Override
    public void upgrade() {
        if (hasCompletedBefore()) {
            LOG.debug("Migration already completed.");
            return;
        }

        final ImmutableSet<String> eventIndexPrefixes = ImmutableSet.of(
                elasticsearchConfig.getDefaultEventsIndexPrefix(),
                elasticsearchConfig.getDefaultSystemEventsIndexPrefix());

        final Set<String> modifiedIndices = elasticsearch.addGl2MessageIdFieldAlias(eventIndexPrefixes);

        writeMigrationCompleted(modifiedIndices);
    }

    private boolean hasCompletedBefore() {
        return clusterConfigService.get(MigrationCompleted.class) != null;
    }

    private void writeMigrationCompleted(Set<String> modifiedIndices) {
        this.clusterConfigService.write(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.create(modifiedIndices));
    }

    public interface ElasticsearchAdapter {
        Set<String> addGl2MessageIdFieldAlias(Set<String> indices);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("modified_indices")
        public abstract Set<String> modifiedIndices();

        @JsonCreator
        public static V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted create(@JsonProperty("modified_indices") final Set<String> modifiedIndices) {
            return new AutoValue_V20200730000000_AddGl2MessageIdFieldAliasForEvents_MigrationCompleted(modifiedIndices);
        }
    }
}
