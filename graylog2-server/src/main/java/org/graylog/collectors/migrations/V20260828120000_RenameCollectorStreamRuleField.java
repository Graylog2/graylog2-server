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
package org.graylog.collectors.migrations;

import jakarta.inject.Inject;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Renames the field of the collector system logs stream rule from {@code collector_receiver_type} to
 * {@code agent_receiver_type}.
 * <p>
 * The message fields stamped by the collector ingest path were renamed to the {@code agent_} prefix to align
 * with the Illuminate GIM {@code agent} entity. {@code CollectorLogsDestinationService} only creates the routing
 * rule once, so existing installations keep the rule on the old field name and collector self-logs would no
 * longer be routed to the system logs stream.
 */
public class V20260828120000_RenameCollectorStreamRuleField extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20260828120000_RenameCollectorStreamRuleField.class);

    static final String OLD_FIELD_NAME = "collector_receiver_type";

    private final ClusterConfigService clusterConfigService;
    private final StreamRuleService streamRuleService;

    @Inject
    public V20260828120000_RenameCollectorStreamRuleField(ClusterConfigService clusterConfigService,
                                                          StreamRuleService streamRuleService) {
        this.clusterConfigService = clusterConfigService;
        this.streamRuleService = streamRuleService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2026-08-28T12:00:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var rules = streamRuleService.loadForStreamId(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID).stream()
                .filter(r -> OLD_FIELD_NAME.equals(r.getField()))
                .toList();

        if (rules.isEmpty()) {
            clusterConfigService.write(new MigrationCompleted(0));
            return;
        }

        rules.forEach(rule -> rule.setField(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE));

        try {
            final var updated = streamRuleService.save(rules);
            clusterConfigService.write(new MigrationCompleted(updated.size()));
            LOG.info("Renamed field <{}> to <{}> on {} collector system logs stream rule(s).",
                    OLD_FIELD_NAME, CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE, updated.size());
        } catch (Exception e) {
            LOG.error("Failed to update collector system logs stream rule field from <{}> to <{}>.",
                    OLD_FIELD_NAME, CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE, e);
            throw new RuntimeException(e);
        }
    }

    public record MigrationCompleted(long modifiedStreamRules) {}
}
