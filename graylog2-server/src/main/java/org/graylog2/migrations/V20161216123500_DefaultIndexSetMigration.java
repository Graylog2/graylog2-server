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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.DefaultIndexSetCreated;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.V20161216123500_Succeeded;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

import static com.google.common.base.Preconditions.checkState;

/**
 * Migration for moving indexing settings into default index set.
 */
public class V20161216123500_DefaultIndexSetMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20161216123500_DefaultIndexSetMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final IndexSetService indexSetService;
    private final ClusterConfigService clusterConfigService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public V20161216123500_DefaultIndexSetMigration(final ElasticsearchConfiguration elasticsearchConfiguration,
                                                    final IndexSetService indexSetService,
                                                    final ClusterConfigService clusterConfigService,
                                                    final ClusterEventBus clusterEventBus) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.indexSetService = indexSetService;
        this.clusterConfigService = clusterConfigService;
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-12-12:35:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20161216123500_Succeeded.class) != null) {
            return;
        }

        // The default index set must have been created first.
        checkState(clusterConfigService.get(DefaultIndexSetCreated.class) != null, "The default index set hasn't been created yet. This is a bug!");

        // TODO: Migrate to new method once it's been merged
        final IndexSetConfig indexSetConfig = indexSetService.getDefault();

        final IndexSetConfig updatedConfig = indexSetConfig.toBuilder()
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(elasticsearchConfiguration.getTemplateName())
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(updatedConfig);
        clusterConfigService.write(V20161216123500_Succeeded.create());

        // Publish event to cluster event bus so the stream router will reload.
        clusterEventBus.post(IndexSetCreatedEvent.create(savedConfig));

        LOG.debug("Successfully updated default index set: {}", savedConfig);
    }
}
