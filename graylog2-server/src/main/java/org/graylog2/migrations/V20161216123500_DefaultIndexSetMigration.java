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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

/**
 * Migration for moving indexing settings into existing index sets.
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
        return ZonedDateTime.of(2016, 12, 16, 12, 35, 0, 0, ZoneOffset.UTC);
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20161216123500_Succeeded.class) != null) {
            return;
        }

        // The default index set must have been created first.
        checkState(clusterConfigService.get(DefaultIndexSetCreated.class) != null, "The default index set hasn't been created yet. This is a bug!");

        final IndexSetConfig defaultIndexSet= indexSetService.getDefault();
        migrateIndexSet(defaultIndexSet, elasticsearchConfiguration.getTemplateName());

        final List<IndexSetConfig> allWithoutDefault = indexSetService.findAll()
                .stream()
                .filter(indexSetConfig -> !indexSetConfig.equals(defaultIndexSet))
                .collect(Collectors.toList());

        for (IndexSetConfig indexSetConfig : allWithoutDefault) {
            migrateIndexSet(indexSetConfig, indexSetConfig.indexPrefix() + "-template");
        }


        clusterConfigService.write(V20161216123500_Succeeded.create());
    }

    private void migrateIndexSet(IndexSetConfig indexSetConfig, String templateName) {
        final String analyzer = elasticsearchConfiguration.getAnalyzer();
        final IndexSetConfig updatedConfig = indexSetConfig.toBuilder()
                .indexAnalyzer(analyzer)
                .indexTemplateName(templateName)
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .build();

        final IndexSetConfig savedConfig = indexSetService.save(updatedConfig);

        // Publish event to cluster event bus so the stream router will reload.
        clusterEventBus.post(IndexSetCreatedEvent.create(savedConfig));

        LOG.debug("Successfully updated index set: {}", savedConfig);
    }
}
