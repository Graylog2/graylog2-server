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
package org.graylog.datanode.configuration;

import com.google.common.eventbus.EventBus;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.OpensearchDistribution;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.graylog.datanode.opensearch.OpensearchStartRequestedEvent;
import org.graylog.datanode.opensearch.bootstrap.InitialClusterManagerNodesResolver;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog.datanode.process.configuration.files.YamlConfigFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class OpensearchConfigurationServiceTest {

    @Mock
    private Configuration localConfiguration;
    @Mock
    private DatanodeConfigurationProvider datanodeConfigurationProvider;
    @Mock
    private EventBus eventBus;
    @Mock
    private OpensearchUpgradeAction opensearchUpgradeAction;
    @Mock
    private OpensearchDistribution opensearchDistribution;
    @Mock
    private InitialClusterManagerNodesResolver initialClusterManagerNodesResolver;

    private OpensearchConfigurationService service;
    private Path dataDir;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dataDir = tempDir;
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(tempDir, tempDir, null, tempDir);
        final DatanodeConfiguration datanodeConfiguration = new DatanodeConfiguration(opensearchDistribution, datanodeDirectories, 100, null);
        when(datanodeConfigurationProvider.get()).thenReturn(datanodeConfiguration);

        service = new OpensearchConfigurationService(localConfiguration, datanodeConfigurationProvider,
                Set.<DatanodeConfigurationBean<OpensearchConfigurationParams>>of(), eventBus, opensearchUpgradeAction, initialClusterManagerNodesResolver);
    }

    @Test
    void startRequestedRebuildsAndPublishesFreshConfiguration() {
        service.onStartRequested(new OpensearchStartRequestedEvent());

        // Starting opensearch (e.g. resuming a previously stopped node) must always resolve a fresh configuration,
        // never reuse whatever was cached from before it stopped.
        verify(datanodeConfigurationProvider).get();
        verify(eventBus).post(any(OpensearchConfigurationChangeEvent.class));
    }

    @Test
    void initialClusterManagerNodesResolvedWhenSecurityConfigured() {
        when(initialClusterManagerNodesResolver.resolve(dataDir)).thenReturn(Optional.of("this_node"));
        service = serviceWithSecurityConfigured(true);

        service.onStartRequested(new OpensearchStartRequestedEvent());

        assertThat(opensearchYml()).containsEntry("cluster.initial_cluster_manager_nodes", "this_node");
    }

    @Test
    void initialClusterManagerNodesOmittedWhenNotBootstrapping() {
        when(initialClusterManagerNodesResolver.resolve(dataDir)).thenReturn(Optional.empty());
        service = serviceWithSecurityConfigured(true);

        service.onStartRequested(new OpensearchStartRequestedEvent());

        assertThat(opensearchYml()).doesNotContainKey("cluster.initial_cluster_manager_nodes");
    }

    @Test
    void noBootstrapClaimWithoutSecurityConfiguration() {
        service = serviceWithSecurityConfigured(false);

        service.onStartRequested(new OpensearchStartRequestedEvent());

        // a node waiting for its certificates won't start opensearch and must not claim the cluster bootstrap
        verify(initialClusterManagerNodesResolver, never()).resolve(any());
        assertThat(opensearchYml()).doesNotContainKey("cluster.initial_cluster_manager_nodes");
    }

    private OpensearchConfigurationService serviceWithSecurityConfigured(boolean securityConfigured) {
        final DatanodeConfigurationBean<OpensearchConfigurationParams> bean = params -> DatanodeConfigurationPart.builder()
                .securityConfigured(securityConfigured)
                .build();
        return new OpensearchConfigurationService(localConfiguration, datanodeConfigurationProvider,
                Set.of(bean), eventBus, opensearchUpgradeAction, initialClusterManagerNodesResolver);
    }

    private Map<String, Object> opensearchYml() {
        final ArgumentCaptor<OpensearchConfigurationChangeEvent> captor = ArgumentCaptor.forClass(OpensearchConfigurationChangeEvent.class);
        verify(eventBus).post(captor.capture());
        return captor.getValue().config().configFiles().stream()
                .filter(YamlConfigFile.class::isInstance)
                .map(YamlConfigFile.class::cast)
                .filter(f -> f.relativePath().equals(Path.of("opensearch.yml")))
                .findFirst()
                .orElseThrow()
                .config();
    }
}
