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
import org.graylog2.database.NotFoundException;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputLauncherTest {
    private static final String INPUT_ID = "input-id";
    private static final String THIS_NODE_ID = "5ca1ab1e-0000-4000-a000-000000000000";
    private static final String OTHER_NODE_ID = "c0c0a000-0000-4000-a000-000000000000";

    private Input input;
    private InputLauncher inputLauncher;
    private InputService inputService;
    private InputRegistry inputRegistry;
    private IOState.Factory<MessageInput> inputStateFactory;

    @BeforeEach
    void setUp() {
        final var inputBuffer = mock(InputBuffer.class);
        final var persistedInputs = mock(PersistedInputs.class);
        final var leaderElectionService = mock(LeaderElectionService.class);
        final var featureFlags = mock(FeatureFlags.class);

        this.input = mock(Input.class);
        this.inputService = mock(InputService.class);
        this.inputRegistry = mock(InputRegistry.class);
        //noinspection unchecked
        this.inputStateFactory = mock(IOState.Factory.class);

        this.inputLauncher = spy(new InputLauncher(
                inputStateFactory,
                inputBuffer,
                persistedInputs,
                inputRegistry,
                new MetricRegistry(),
                new Configuration(),
                leaderElectionService,
                featureFlags,
                inputService,
                new SimpleNodeId(THIS_NODE_ID)
        ));
    }

    @Test
    void launchDoesNothingIfInputDoesNotExist() throws Exception {
        when(inputService.find(INPUT_ID)).thenThrow(NotFoundException.class);

        inputLauncher.launch(INPUT_ID);

        verifyNoInteractions(inputStateFactory, inputRegistry);
    }

    @Test
    void launchDoesNothingIfInputTypeIsMissing() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);
        when(inputService.getMessageInput(input)).thenThrow(NoSuchInputTypeException.class);

        inputLauncher.launch(INPUT_ID);

        verify(inputLauncher, never()).launch(any(MessageInput.class));
        verifyNoInteractions(inputStateFactory, inputRegistry);
    }

    @Test
    void launchWithBlankInputIdFails() {
        final String inputId = null;
        assertThatThrownBy(() -> inputLauncher.launch(inputId)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inputLauncher.launch("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> inputLauncher.launch("     ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void launchWithNullMessageInputFails() {
        final MessageInput messageInput = null;
        assertThatThrownBy(() -> inputLauncher.launch(messageInput)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void launchDoesNotStartLocalInputOnAnyNode() throws Exception {
        when(inputService.find(INPUT_ID)).thenReturn(input);

        final MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getNodeId()).thenReturn(OTHER_NODE_ID);
        when(messageInput.isGlobal()).thenReturn(false);
        when(inputService.getMessageInput(input)).thenReturn(messageInput);

        inputLauncher.launch(INPUT_ID);

        verifyNoInteractions(inputStateFactory, inputRegistry);
    }
}
