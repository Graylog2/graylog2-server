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
package org.graylog2.migrations;

import jakarta.inject.Inject;
import org.graylog.aws.inputs.cloudtrail.CloudTrailInput;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Migration to add default values for new CloudTrail input configuration fields.
 *
 * Adds:
 * - polling_interval (default: 1)
 * - sqs_message_batch_size (default: 5)
 */
public class V20251030000000_CloudTrailInputConfigMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251030000000_CloudTrailInputConfigMigration.class);

    private final ClusterConfigService clusterConfigService;
    private final InputService inputService;

    @Inject
    public V20251030000000_CloudTrailInputConfigMigration(ClusterConfigService clusterConfigService,
                                                          InputService inputService) {
        this.clusterConfigService = clusterConfigService;
        this.inputService = inputService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-10-30T00:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }
        try (final Stream<Input> inputs = inputService.allByType(CloudTrailInput.TYPE).stream()) {
            inputs.forEach(input -> {
                try {
                    final Map<String, Object> config = input.getConfiguration();
                    boolean needsUpdate = false;

                    // Add default value for polling_interval if not present
                    if (!config.containsKey(CloudTrailInput.CK_POLLING_INTERVAL)) {
                        config.put(CloudTrailInput.CK_POLLING_INTERVAL, 1);
                        needsUpdate = true;
                    }

                    // Add default value for sqs_message_batch_size if not present
                    if (!config.containsKey(CloudTrailInput.CK_SQS_MESSAGE_BATCH_SIZE)) {
                        config.put(CloudTrailInput.CK_SQS_MESSAGE_BATCH_SIZE, 5);
                        needsUpdate = true;
                    }

                    // Remove codec-level charset_name field if present (artifact from previous saves)
                    if (config.containsKey(Codec.Config.CK_CHARSET_NAME)) {
                        config.remove(Codec.Config.CK_CHARSET_NAME);
                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        inputService.save(input);
                        LOG.info("Successfully migrated CloudTrail input [{}/{}].",
                                input.getTitle(), input.getId());
                    }
                } catch (Exception e) {
                    LOG.error("An error occurred migrating CloudTrail input [{}/{}].",
                            input.getTitle(), input.getId(), e);
                }
            });
        }

        clusterConfigService.write(new MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
