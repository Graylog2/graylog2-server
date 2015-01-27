/**
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
 */
package org.graylog2.system.stats;

import org.graylog2.bundles.BundleService;
import org.graylog2.dashboards.DashboardService;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.system.stats.elasticsearch.ElasticsearchProbe;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.mongo.MongoProbe;
import org.graylog2.system.stats.mongo.MongoStats;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClusterStatsService {
    private final ElasticsearchProbe elasticsearchProbe;
    private final MongoProbe mongoProbe;
    private final UserService userService;
    private final InputService inputService;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final OutputService outputService;
    private final DashboardService dashboardService;
    private final BundleService bundleService;

    @Inject
    public ClusterStatsService(ElasticsearchProbe elasticsearchProbe,
                               MongoProbe mongoProbe,
                               UserService userService,
                               InputService inputService,
                               StreamService streamService,
                               StreamRuleService streamRuleService,
                               OutputService outputService,
                               DashboardService dashboardService,
                               BundleService bundleService) {
        this.elasticsearchProbe = elasticsearchProbe;
        this.mongoProbe = mongoProbe;
        this.userService = userService;
        this.inputService = inputService;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.outputService = outputService;
        this.dashboardService = dashboardService;
        this.bundleService = bundleService;
    }

    public ClusterStats clusterStats() {
        return ClusterStats.create(
                elasticsearchStats(),
                mongoStats(),
                streamService.count(),
                streamRuleService.totalStreamRuleCount(),
                streamRuleService.streamRuleCountByStream(),
                userService.count(),
                outputService.count(),
                outputService.countByType(),
                dashboardService.count(),
                inputService.totalCount(),
                inputService.globalCount(),
                inputService.totalCountByType(),
                inputService.totalExtractorCount(),
                inputService.totalExtractorCountByType(),
                bundleService.count()
        );
    }

    public ElasticsearchStats elasticsearchStats() {
        return elasticsearchProbe.elasticsearchStats();
    }

    public MongoStats mongoStats() {
        return mongoProbe.mongoStats();
    }
}
