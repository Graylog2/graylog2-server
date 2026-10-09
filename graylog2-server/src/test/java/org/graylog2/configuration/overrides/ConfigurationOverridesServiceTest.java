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
package org.graylog2.configuration.overrides;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
class ConfigurationOverridesServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    private final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);
    private ConfigurationOverridesService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        final var clusterConfigService = new ClusterConfigServiceImpl(
                new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()),
                mongoCollections.connection(),
                new SimpleNodeId("node-1"),
                new RestrictedChainingClassLoader(new ChainingClassLoader(getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()),
                clusterEventBus
        );
        service = new ConfigurationOverridesService(mongoCollections, clusterConfigService, clusterEventBus,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void returnsNoOverridesInitially() {
        assertThat(service.getOverrides(NodeType.SERVER, null)).isEmpty();
        assertThat(service.getOverrides(NodeType.SERVER, "node-1")).isEmpty();
    }

    @Test
    void setsNodeOverrides() {
        final var result = service.updateOverrides(NodeType.SERVER, "node-1", Map.of("b", "2", "a", "1"), "admin");

        assertThat(result).containsExactly(
                Map.entry("a", new ConfigurationOverrideValue("1", NOW, "admin")),
                Map.entry("b", new ConfigurationOverrideValue("2", NOW, "admin")));
        assertThat(service.getOverrides(NodeType.SERVER, "node-1")).isEqualTo(result);
        verify(clusterEventBus).post(new NodeConfigurationOverridesChangedEvent("node-1", Set.of("a", "b")));
    }

    @Test
    void mergesNodeOverridesAndRemovesNullValues() {
        service.updateOverrides(NodeType.SERVER, "node-1", Map.of("a", "1", "b", "2", "c", "3"), "admin");

        final var changes = new HashMap<String, String>();
        changes.put("a", "10");
        changes.put("b", null);
        final var result = service.updateOverrides(NodeType.SERVER, "node-1", changes, "jane");

        assertThat(result).containsOnlyKeys("a", "c");
        assertThat(result.get("a")).isEqualTo(new ConfigurationOverrideValue("10", NOW, "jane"));
        assertThat(result.get("c")).isEqualTo(new ConfigurationOverrideValue("3", NOW, "admin"));
    }

    @Test
    void keepsScopesSeparate() {
        service.updateOverrides(NodeType.SERVER, "node-1", Map.of("a", "node-1"), "admin");
        service.updateOverrides(NodeType.SERVER, "node-2", Map.of("a", "node-2"), "admin");
        service.updateOverrides(NodeType.SERVER, null, Map.of("a", "all servers"), "admin");
        service.updateOverrides(NodeType.DATANODE, null, Map.of("a", "all data nodes"), "admin");

        assertThat(service.getOverrides(NodeType.SERVER, "node-1").get("a").value()).isEqualTo("node-1");
        assertThat(service.getOverrides(NodeType.SERVER, "node-2").get("a").value()).isEqualTo("node-2");
        assertThat(service.getOverrides(NodeType.SERVER, null).get("a").value()).isEqualTo("all servers");
        assertThat(service.getOverrides(NodeType.DATANODE, null).get("a").value()).isEqualTo("all data nodes");
        assertThat(service.getOverrides(NodeType.DATANODE, "node-1")).isEmpty();
    }

    @Test
    void mergesNodeTypeOverridesAndRemovesNullValues() {
        service.updateOverrides(NodeType.SERVER, null, Map.of("a", "1", "b", "2"), "admin");

        final var changes = new HashMap<String, String>();
        changes.put("b", null);
        changes.put("c", "3");
        final var result = service.updateOverrides(NodeType.SERVER, null, changes, "jane");

        assertThat(result).containsExactly(
                Map.entry("a", new ConfigurationOverrideValue("1", NOW, "admin")),
                Map.entry("c", new ConfigurationOverrideValue("3", NOW, "jane")));
        assertThat(service.getOverrides(NodeType.SERVER, null)).isEqualTo(result);
    }

    @Test
    void doesNothingWithoutChanges() {
        assertThat(service.updateOverrides(NodeType.SERVER, "node-1", Map.of(), "admin")).isEmpty();

        verify(clusterEventBus, never()).post(any());
    }
}
