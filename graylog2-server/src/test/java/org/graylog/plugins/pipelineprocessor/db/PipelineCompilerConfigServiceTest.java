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
package org.graylog.plugins.pipelineprocessor.db;

import org.graylog.plugins.pipelineprocessor.events.PipelineCompilerConfigChangedEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineCompilerConfigServiceTest {
    private final ClusterConfigService clusterConfig = mock(ClusterConfigService.class);
    private final ClusterEventBus clusterEventBus = mock(ClusterEventBus.class);
    private final PipelineCompilerConfigService service = new PipelineCompilerConfigService(clusterConfig, clusterEventBus);

    @Test
    void defaultsToDisabled() {
        when(clusterConfig.getOrDefault(eq(PipelineCompilerConfigDto.class), any())).thenAnswer(i -> i.getArgument(1));
        assertThat(service.get().enabled()).isFalse();
    }

    @Test
    void saveWritesConfigAndPostsChangeEvent() {
        when(clusterConfig.getOrDefault(eq(PipelineCompilerConfigDto.class), any())).thenReturn(PipelineCompilerConfigDto.create(true));
        final PipelineCompilerConfigDto saved = service.save(PipelineCompilerConfigDto.create(true));
        verify(clusterConfig).write(PipelineCompilerConfigDto.create(true));
        verify(clusterEventBus).post(PipelineCompilerConfigChangedEvent.create(true));
        assertThat(saved.enabled()).isTrue();
    }
}
