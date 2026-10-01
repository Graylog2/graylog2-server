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
package org.graylog.datanode.opensearch.bootstrap;

import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchNodeRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialClusterManagerNodesResolverTest {

    private final Configuration configuration = mock(Configuration.class);
    private final ClusterBootstrapService bootstrapService = mock(ClusterBootstrapService.class);
    private final InitialClusterManagerNodesResolver resolver = new InitialClusterManagerNodesResolver(configuration, bootstrapService);

    @Test
    void bootstrappingNodeIsTheOnlyInitialManager(@TempDir Path dataDir) {
        when(configuration.getDatanodeNodeName()).thenReturn("node1");
        when(bootstrapService.claimBootstrap()).thenReturn(true);

        assertThat(resolver.resolve(dataDir)).hasValue("node1");
    }

    @Test
    void joiningNodeHasNoInitialManagers(@TempDir Path dataDir) {
        when(bootstrapService.claimBootstrap()).thenReturn(false);

        assertThat(resolver.resolve(dataDir)).isEmpty();
    }

    @Test
    void explicitConfigurationWins(@TempDir Path dataDir) {
        when(configuration.getInitialClusterManagerNodes()).thenReturn("node1,node2");

        assertThat(resolver.resolve(dataDir)).hasValue("node1,node2");
        verify(bootstrapService).registerExplicitBootstrap("node1,node2");
        verify(bootstrapService, never()).claimBootstrap();
    }

    @Test
    void explicitConfigurationOfFormedNodeIsNotRegistered(@TempDir Path dataDir) throws IOException {
        when(configuration.getInitialClusterManagerNodes()).thenReturn("node1,node2");
        Files.createDirectories(dataDir.resolve("nodes").resolve("0").resolve("_state"));

        assertThat(resolver.resolve(dataDir)).hasValue("node1,node2");
        verify(bootstrapService, never()).registerExplicitBootstrap("node1,node2");
    }

    @Test
    void nodeWithoutManagerRoleNeverBootstraps(@TempDir Path dataDir) {
        when(configuration.getNodeRoles()).thenReturn(List.of(OpensearchNodeRole.DATA));

        assertThat(resolver.resolve(dataDir)).isEmpty();
        verify(bootstrapService, never()).claimBootstrap();
    }

    @Test
    void nodeWithClusterStateNeverBootstraps(@TempDir Path dataDir) throws IOException {
        Files.createDirectories(dataDir.resolve("nodes").resolve("0").resolve("_state"));

        assertThat(resolver.resolve(dataDir)).isEmpty();
        verify(bootstrapService, never()).claimBootstrap();
    }

    @Test
    void detectsClusterState(@TempDir Path dataDir) throws IOException {
        assertThat(InitialClusterManagerNodesResolver.hasClusterState(dataDir)).isFalse();

        Files.createDirectories(dataDir.resolve("nodes").resolve("0"));
        assertThat(InitialClusterManagerNodesResolver.hasClusterState(dataDir)).isFalse();

        Files.createDirectories(dataDir.resolve("nodes").resolve("0").resolve("_state"));
        assertThat(InitialClusterManagerNodesResolver.hasClusterState(dataDir)).isTrue();
    }
}
