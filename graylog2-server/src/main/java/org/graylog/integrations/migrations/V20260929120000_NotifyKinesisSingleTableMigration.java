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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import org.graylog.integrations.aws.inputs.AWSInput;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.migrations.Migration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.web.customization.CustomizationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Raises a system notification about the optional KCL 3.5 single-table migration when Kinesis inputs without the
 * migration option enabled exist at upgrade time. Inputs created on 7.2.0 also count, since they don't set the option
 * even though they already use a single table; the notification text tells users only pre-7.2 inputs need migrating.
 */
public class V20260929120000_NotifyKinesisSingleTableMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260929120000_NotifyKinesisSingleTableMigration.class);

    static final String NOTIFICATION_KEY = "kinesis_single_table_migration";
    static final String PRODUCT_NAME_DETAIL = "product_name";

    private final ClusterConfigService clusterConfigService;
    private final InputService inputService;
    private final NotificationService notificationService;
    private final CustomizationConfig customizationConfig;

    @Inject
    public V20260929120000_NotifyKinesisSingleTableMigration(ClusterConfigService clusterConfigService,
                                                             InputService inputService,
                                                             NotificationService notificationService,
                                                             CustomizationConfig customizationConfig) {
        this.clusterConfigService = clusterConfigService;
        this.inputService = inputService;
        this.notificationService = notificationService;
        this.customizationConfig = customizationConfig;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-09-29T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final Set<String> unmigratedInputIds = inputService.allByType(AWSInput.TYPE).stream()
                .filter(input -> !usesSingleTable(input))
                .map(Input::getId)
                .collect(Collectors.toSet());

        if (!unmigratedInputIds.isEmpty()) {
            notificationService.publishIfFirst(notificationService.buildNow()
                    .addType(Notification.Type.KINESIS_SINGLE_TABLE_MIGRATION)
                    .addKey(NOTIFICATION_KEY)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail(PRODUCT_NAME_DETAIL, customizationConfig.productName()));
            LOG.info("Raised the KCL single-table migration notification for {} existing AWS Kinesis/CloudWatch input(s).",
                    unmigratedInputIds.size());
        }

        clusterConfigService.write(new MigrationCompleted(unmigratedInputIds));
    }

    // Inputs that already have the migration option enabled have nothing left to migrate.
    private static boolean usesSingleTable(Input input) {
        return Boolean.parseBoolean(String.valueOf(
                input.getConfiguration().get(KinesisTransport.CK_KINESIS_SINGLE_TABLE_STATE_TRACKING)));
    }

    public record MigrationCompleted(@JsonProperty("unmigrated_input_ids") Set<String> unmigratedInputIds) {}
}
