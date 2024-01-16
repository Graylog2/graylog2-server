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
package org.graylog.integrations.migrations;

import org.graylog.integrations.dataadapters.GreyNoiseCommunityIpLookupAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.migrations.Migration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.time.ZonedDateTime;
import java.util.List;

public class V20230522201200_NotificationForDeprecatedGreyNoiseCommunityDataAdapters extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230522201200_NotificationForDeprecatedGreyNoiseCommunityDataAdapters.class);

    private final ClusterConfigService clusterConfigService;
    private final DBDataAdapterService dataAdapterService;
    private final NotificationService notificationService;

    @Inject
    public V20230522201200_NotificationForDeprecatedGreyNoiseCommunityDataAdapters(ClusterConfigService clusterConfigService,
                                                                                   DBDataAdapterService dataAdapterService,
                                                                                   NotificationService notificationService) {
        this.clusterConfigService = clusterConfigService;
        this.dataAdapterService = dataAdapterService;
        this.notificationService = notificationService;
    }

    /**
     * This migration notifies users of the deprecation and removal of functionality for GreyNoiseCommunityIpLookupAdapters.
     */
    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20230522201200_NotificationForDeprecatedGreyNoiseCommunityDataAdapters.MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
            return;
        }

        List<String> greyNoiseCommunityAdapters = dataAdapterService.findAll().stream()
                .filter(da -> da.config().type().equals(GreyNoiseCommunityIpLookupAdapter.ADAPTER_NAME))
                .map(da -> da.name()).toList();

        if (!greyNoiseCommunityAdapters.isEmpty()) {
            final Notification systemNotification = notificationService.buildNow()
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("title", StringUtils.f("Disabled Data Adapters %s", greyNoiseCommunityAdapters.toString()))
                    .addDetail("description", "GreyNoise Community IP Lookup Data Adapters are no longer supported as of Graylog 5.2."
                            + " GreyNoise Community Data Adapters will no longer return results and should be deleted.");

            notificationService.publishIfFirst(systemNotification);
        }

        clusterConfigService.write(new V20230522201200_NotificationForDeprecatedGreyNoiseCommunityDataAdapters.MigrationCompleted());
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-05-22T20:12:00Z");
    }

    public record MigrationCompleted() {}
}
