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
package org.graylog2.shared.inputs;

import com.codahale.metrics.MetricRegistry;
import org.graylog2.Configuration;
import org.graylog2.cluster.leader.LeaderElectionService;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InputLauncherTest {
    @Mock
    private IOState.Factory<MessageInput> inputStateFactory;

    @Mock
    private InputBuffer inputBuffer;

    @Mock
    private PersistedInputs persistedInputs;

    @Mock
    private InputRegistry inputRegistry;

    @Mock
    private Configuration configuration;

    @Mock
    private LeaderElectionService leaderElectionService;

    @Mock
    private FeatureFlags featureFlags;

    private InputLauncher launcher;

    @BeforeEach
    void setUp() {
        launcher = new InputLauncher(inputStateFactory, inputBuffer, persistedInputs, inputRegistry,
                new MetricRegistry(), configuration, leaderElectionService, featureFlags);
    }

    @Test
    void allPersistedLaunchedTurnsTrueOnlyOnceTheLaunchPassHasRun() {
        when(persistedInputs.iterator()).thenReturn(List.<MessageInput>of().iterator());

        assertThat(launcher.allPersistedLaunched()).isFalse();

        launcher.launchAllPersisted();

        assertThat(launcher.allPersistedLaunched()).isTrue();
    }

    @Test
    void allPersistedLaunchedTurnsTrueEvenWhenTheLaunchPassAborts() {
        final MessageInput input = mock(MessageInput.class);
        when(input.onlyOnePerCluster()).thenReturn(false);
        when(input.getDesiredState()).thenReturn(IOState.Type.RUNNING);
        doThrow(new IllegalStateException("cannot build the transport metrics")).when(input).initialize();
        when(persistedInputs.iterator()).thenReturn(List.of(input).iterator());

        assertThatThrownBy(() -> launcher.launchAllPersisted()).isInstanceOf(IllegalStateException.class);

        assertThat(launcher.allPersistedLaunched()).isTrue();
    }

    @Test
    void allPersistedLaunchedTurnsTrueEvenWhenThePersistedInputLookupFails() {
        when(persistedInputs.iterator()).thenThrow(new IllegalStateException("MongoDB is unreachable"));

        assertThatThrownBy(() -> launcher.launchAllPersisted()).isInstanceOf(IllegalStateException.class);

        assertThat(launcher.allPersistedLaunched()).isTrue();
    }
}
