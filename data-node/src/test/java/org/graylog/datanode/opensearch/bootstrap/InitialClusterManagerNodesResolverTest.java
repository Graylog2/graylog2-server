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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialClusterManagerNodesResolverTest {

    private final Configuration configuration = mock(Configuration.class);
    private final ClusterBootstrapService bootstrapService = mock(ClusterBootstrapService.class);
    private final InitialClusterManagerNodesResolver resolver = new InitialClusterManagerNodesResolver(configuration, bootstrapService);

    @Test
    void bootstrappingNodeIsTheOnlyInitialManager() {
        when(configuration.getDatanodeNodeName()).thenReturn("node1");
        when(bootstrapService.claimBootstrap()).thenReturn(true);

        assertThat(resolver.resolve()).hasValue("node1");
    }

    @Test
    void joiningNodeHasNoInitialManagers() {
        when(bootstrapService.claimBootstrap()).thenReturn(false);

        assertThat(resolver.resolve()).isEmpty();
    }

    @Test
    void explicitConfigurationWins() {
        when(configuration.getInitialClusterManagerNodes()).thenReturn("node1,node2");

        assertThat(resolver.resolve()).hasValue("node1,node2");
        verify(bootstrapService).registerExplicitBootstrap("node1,node2");
        verify(bootstrapService, never()).claimBootstrap();
    }

    @Test
    void nodeWithoutManagerRoleNeverBootstraps() {
        when(configuration.getNodeRoles()).thenReturn(List.of(OpensearchNodeRole.DATA));

        assertThat(resolver.resolve()).isEmpty();
        verify(bootstrapService, never()).claimBootstrap();
    }

    @Test
    void takesOverExpiredBootstrap() {
        when(bootstrapService.takeOverExpiredClaim()).thenReturn(true);

        assertThat(resolver.takeOverExpiredBootstrap()).isTrue();
    }

    @Test
    void neverTakesOverWithoutManagerRoleOrWithExplicitConfiguration() {
        when(bootstrapService.takeOverExpiredClaim()).thenReturn(true);

        when(configuration.getNodeRoles()).thenReturn(List.of(OpensearchNodeRole.DATA));
        assertThat(resolver.takeOverExpiredBootstrap()).isFalse();

        when(configuration.getNodeRoles()).thenReturn(List.of());
        when(configuration.getInitialClusterManagerNodes()).thenReturn("node1,node2");
        assertThat(resolver.takeOverExpiredBootstrap()).isFalse();

        verify(bootstrapService, never()).takeOverExpiredClaim();
    }

    @Test
    void explicitConfigurationOfNodeWithoutManagerRoleIsNotRegistered() {
        when(configuration.getNodeRoles()).thenReturn(List.of(OpensearchNodeRole.DATA));
        when(configuration.getInitialClusterManagerNodes()).thenReturn("node1,node2");

        assertThat(resolver.resolve()).hasValue("node1,node2");
        verify(bootstrapService, never()).registerExplicitBootstrap(any());
    }
}
