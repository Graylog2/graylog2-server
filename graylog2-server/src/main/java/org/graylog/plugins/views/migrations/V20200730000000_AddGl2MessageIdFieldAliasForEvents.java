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

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.Set;

public class V20200730000000_AddGl2MessageIdFieldAliasForEvents extends Migration {

    private static final Logger LOG = LoggerFactory.getLogger(V20200730000000_AddGl2MessageIdFieldAliasForEvents.class);

    private static final int MINIMUM_ELASTICSEARCH_VERSION = 7;

    private final String elasticsearchMajorVersion;
    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchAdapter elasticsearch;
    private final ElasticsearchConfiguration elasticsearchConfig;

    @Inject
    public V20200730000000_AddGl2MessageIdFieldAliasForEvents(
            @Named("elasticsearch_version") String elasticsearchMajorVersion,
            ClusterConfigService clusterConfigService,
            ElasticsearchAdapter elasticsearch,
            ElasticsearchConfiguration elasticsearchConfig) {
        this.elasticsearchMajorVersion = elasticsearchMajorVersion;
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
        if (shouldSkip()) {
            return;
        }

        final ImmutableSet<String> eventIndexPrefixes = ImmutableSet.of(
                elasticsearchConfig.getDefaultEventsIndexPrefix(),
                elasticsearchConfig.getDefaultSystemEventsIndexPrefix());

        elasticsearch.addGl2MessageIdFieldAlias(eventIndexPrefixes);

        writeMigrationCompleted(eventIndexPrefixes);
    }

    private boolean shouldSkip() {
        if (Integer.parseInt(elasticsearchMajorVersion) < MINIMUM_ELASTICSEARCH_VERSION) {
            LOG.debug("Skipping migration, because Elasticsearch major version of {} " +
                            "is lower than the required minimum version of {}.",
                    elasticsearchMajorVersion, MINIMUM_ELASTICSEARCH_VERSION);
            return true;
        }
        if (hasCompletedBefore()) {
            LOG.debug("Migration already completed.");
            return true;
        }
        return false;
    }

    private boolean hasCompletedBefore() {
        return clusterConfigService.get(MigrationCompleted.class) != null;
    }

    private void writeMigrationCompleted(ImmutableSet<String> eventIndexPrefixes) {
        this.clusterConfigService.write(V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted.create(eventIndexPrefixes));
    }

    public interface ElasticsearchAdapter {
        void addGl2MessageIdFieldAlias(Set<String> indexPrefixes);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("modified_index_prefixes")
        public abstract Set<String> modifiedIndexPrefixes();

        @JsonCreator
        public static V20200730000000_AddGl2MessageIdFieldAliasForEvents.MigrationCompleted create(@JsonProperty("modified_index_prefixes") final Set<String> modifiedIndexPrefixes) {
            return new AutoValue_V20200730000000_AddGl2MessageIdFieldAliasForEvents_MigrationCompleted(modifiedIndexPrefixes);
        }
    }
}
