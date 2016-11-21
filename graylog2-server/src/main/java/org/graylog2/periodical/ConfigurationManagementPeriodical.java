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

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.migrations.V20151210140600_ElasticsearchConfigMigration;
import org.graylog2.migrations.V20160929120500_CreateDefaultStreamMigration;
import org.graylog2.migrations.V20161116172100_DefaultIndexSetMigration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class ConfigurationManagementPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManagementPeriodical.class);
    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final ClusterConfigService clusterConfigService;
    private final StreamService streamService;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final IndexSetService indexSetService;
    private final ClusterEventBus clusterEventBus;

    @Inject
    public ConfigurationManagementPeriodical(ElasticsearchConfiguration elasticsearchConfiguration,
                                             ClusterConfigService clusterConfigService,
                                             StreamService streamService,
                                             Map<String, Provider<RotationStrategy>> rotationStrategies,
                                             Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                             IndexSetService indexSetService,
                                             ClusterEventBus clusterEventBus) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.clusterConfigService = clusterConfigService;
        this.streamService = streamService;
        this.rotationStrategies = rotationStrategies;
        this.retentionStrategies = retentionStrategies;
        this.indexSetService = indexSetService;
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public void doRun() {
        new V20151210140600_ElasticsearchConfigMigration(clusterConfigService, elasticsearchConfiguration).upgrade();
        new V20160929120500_CreateDefaultStreamMigration(streamService, clusterEventBus).upgrade();
        new V20161116172100_DefaultIndexSetMigration(
                elasticsearchConfiguration,
                rotationStrategies,
                retentionStrategies,
                indexSetService,
                clusterConfigService,
                clusterEventBus
        ).upgrade();
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
}
