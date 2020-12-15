/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.system.stats;

import org.graylog.plugins.views.search.views.DashboardService;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alerts.AlertService;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.mongo.MongoProbe;
import org.graylog2.system.stats.mongo.MongoStats;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class ClusterStatsService {
    private final MongoProbe mongoProbe;
    private final UserService userService;
    private final InputService inputService;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final OutputService outputService;
    private final AlertService alertService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final DashboardService dashboardService;
    private final Cluster cluster;

    @Inject
    public ClusterStatsService(MongoProbe mongoProbe,
                               UserService userService,
                               InputService inputService,
                               StreamService streamService,
                               StreamRuleService streamRuleService,
                               OutputService outputService,
                               AlertService alertService,
                               AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                               DashboardService dashboardService,
                               Cluster cluster) {
        this.mongoProbe = mongoProbe;
        this.userService = userService;
        this.inputService = inputService;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.outputService = outputService;
        this.alertService = alertService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.dashboardService = dashboardService;
        this.cluster = cluster;
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
                countDashboards(),
                inputService.totalCount(),
                inputService.globalCount(),
                inputService.totalCountByType(),
                inputService.totalExtractorCount(),
                inputService.totalExtractorCountByType(),
                alarmStats()
        );
    }

    private long countDashboards() {
        return dashboardService.count();
    }

    public ElasticsearchStats elasticsearchStats() {
        return cluster.elasticsearchStats();
    }

    public MongoStats mongoStats() {
        return mongoProbe.mongoStats();
    }

    public AlarmStats alarmStats() {
        final long totalCount = alertService.totalCount();
        final Map<String, Long> counterPerType = alarmCallbackConfigurationService.countPerType();
        return AlarmStats.create(totalCount, counterPerType);
    }
}
