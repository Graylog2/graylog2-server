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

import com.google.common.eventbus.Subscribe;
import org.bson.types.ObjectId;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleServiceImpl;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class V20260828120000_RenameCollectorStreamRuleFieldTest {

    private static final String OTHER_STREAM_ID = "000000000000000000000099";

    private final NodeId nodeId = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private StreamRuleServiceImpl streamRuleService;
    private StreamsChangedEventCollector eventCollector;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, MongoJackObjectMapperProvider objectMapperProvider) {
        final MongoConnection connection = mongoCollections.connection();
        final ClusterEventBus clusterEventBus = new ClusterEventBus();
        this.eventCollector = new StreamsChangedEventCollector();
        clusterEventBus.registerClusterEventSubscriber(eventCollector);

        this.clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider,
                connection,
                nodeId,
                new RestrictedChainingClassLoader(
                        new ChainingClassLoader(getClass().getClassLoader()), SafeClasses.allGraylogInternal()),
                clusterEventBus);

        this.streamRuleService = new StreamRuleServiceImpl(connection, clusterEventBus);
        this.migration = new V20260828120000_RenameCollectorStreamRuleField(clusterConfigService, streamRuleService);
    }

    @Test
    void createdAt() {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2026-08-28T12:00:00Z"));
    }

    @Test
    void upgradeRenamesOnlyTheSystemLogsStreamRule() throws Exception {
        final String systemRuleId = createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);
        final String unrelatedRuleId = createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID, "source");
        final String otherStreamRuleId = createRule(OTHER_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);

        migration.upgrade();

        final StreamRule systemRule = streamRuleService.load(systemRuleId);
        assertThat(systemRule.getField()).isEqualTo(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE);
        assertThat(systemRule.getValue()).isEqualTo("collector_log");
        assertThat(systemRule.getType()).isEqualTo(StreamRuleType.EXACT);

        assertThat(streamRuleService.load(unrelatedRuleId).getField()).isEqualTo("source");
        assertThat(streamRuleService.load(otherStreamRuleId).getField())
                .isEqualTo(V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);

        assertThat(streamRuleService.totalStreamRuleCount()).isEqualTo(3L);

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isEqualTo(1L);
    }

    @Test
    void upgradeRenamesAllMatchingStreamRules() throws Exception {
        final String firstRuleId = createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);
        final String secondRuleId = createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);

        migration.upgrade();

        assertThat(streamRuleService.load(firstRuleId).getField())
                .isEqualTo(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE);
        assertThat(streamRuleService.load(secondRuleId).getField())
                .isEqualTo(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE);

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isEqualTo(2L);
    }

    @Test
    void upgradeIsIdempotent() throws Exception {
        final String ruleId = createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);

        migration.upgrade();
        eventCollector.events.clear();
        migration.upgrade();

        assertThat(streamRuleService.load(ruleId).getField()).isEqualTo(CollectorIngestCodec.FIELD_AGENT_RECEIVER_TYPE);
        assertThat(eventCollector.events).isEmpty();

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isEqualTo(1L);
    }

    @Test
    void upgradeWithoutStreamRulesCompletes() {
        migration.upgrade();

        assertThat(eventCollector.events).isEmpty();

        final var completed = clusterConfigService.get(V20260828120000_RenameCollectorStreamRuleField.MigrationCompleted.class);
        assertThat(completed).isNotNull();
        assertThat(completed.modifiedStreamRules()).isZero();
    }

    @Test
    void upgradePostsStreamsChangedEvent() throws Exception {
        createRule(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID,
                V20260828120000_RenameCollectorStreamRuleField.OLD_FIELD_NAME);
        eventCollector.events.clear();

        migration.upgrade();

        assertThat(eventCollector.events).hasSize(1);
        assertThat(eventCollector.events.get(0).streamIds()).containsExactly(Stream.COLLECTOR_SYSTEM_LOGS_STREAM_ID);
    }

    private String createRule(String streamId, String field) throws ValidationException {
        final StreamRule rule = streamRuleService.create(Map.of(
                StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamId),
                StreamRuleImpl.FIELD_FIELD, field,
                StreamRuleImpl.FIELD_TYPE, StreamRuleType.EXACT.toInteger(),
                StreamRuleImpl.FIELD_VALUE, "collector_log",
                StreamRuleImpl.FIELD_INVERTED, false,
                StreamRuleImpl.FIELD_DESCRIPTION, "Route collector system logs to dedicated stream"
        ));
        return streamRuleService.save(rule);
    }

    private static class StreamsChangedEventCollector {
        private final List<StreamsChangedEvent> events = new ArrayList<>();

        @Subscribe
        public void onStreamsChanged(StreamsChangedEvent event) {
            events.add(event);
        }
    }
}
