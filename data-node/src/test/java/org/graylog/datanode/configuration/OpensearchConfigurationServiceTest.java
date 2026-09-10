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
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
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

    private OpensearchConfigurationService service;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        final DatanodeDirectories datanodeDirectories = new DatanodeDirectories(tempDir, tempDir, null, tempDir);
        final DatanodeConfiguration datanodeConfiguration = new DatanodeConfiguration(opensearchDistribution, datanodeDirectories, 100, null);
        when(datanodeConfigurationProvider.get()).thenReturn(datanodeConfiguration);

        service = new OpensearchConfigurationService(localConfiguration, datanodeConfigurationProvider,
                Set.<DatanodeConfigurationBean<OpensearchConfigurationParams>>of(), eventBus, opensearchUpgradeAction);
    }

    @Test
    void startRequestedRebuildsAndPublishesFreshConfiguration() {
        service.onStartRequested(new OpensearchStartRequestedEvent());

        // Starting opensearch (e.g. resuming a previously stopped node) must always resolve a fresh configuration,
        // never reuse whatever was cached from before it stopped.
        verify(datanodeConfigurationProvider).get();
        verify(eventBus).post(any(OpensearchConfigurationChangeEvent.class));
    }
}
