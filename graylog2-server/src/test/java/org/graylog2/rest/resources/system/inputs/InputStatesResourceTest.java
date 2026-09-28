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
package org.graylog2.rest.resources.system.inputs;

import com.google.common.eventbus.EventBus;
import jakarta.ws.rs.NotFoundException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.persistence.InputStateService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.inputs.InputLauncher;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(permissions = {"*"})
@MockitoSettings(strictness = Strictness.LENIENT)
class InputStatesResourceTest {
    private static final NodeId NODE_ID = new SimpleNodeId("5ca1ab1e-0000-4000-a000-000000000000");
    private static final String RUNNING_ID = "000000000000000000000001";
    private static final String STOPPED_ID = "000000000000000000000002";
    private static final String BROKEN_ID = "000000000000000000000003";
    private static final DateTime CREATED_AT = new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC);

    @Mock
    private InputRegistry inputRegistry;

    @Mock
    private InputService inputService;

    @Mock
    private InputStateService inputStateService;

    @Mock
    private InputLauncher inputLauncher;

    @Mock
    private MessageInputFactory messageInputFactory;

    private final List<Input> configured = new ArrayList<>();

    private InputStatesResource resource;

    @BeforeEach
    void setUp() {
        when(messageInputFactory.getAvailableInputs()).thenReturn(Map.of());
        when(inputLauncher.allPersistedLaunched()).thenReturn(true);
        when(inputRegistry.stream()).thenAnswer(i -> Stream.empty());
        when(inputService.findIdsForThisNodeOrGlobal(NODE_ID.getNodeId()))
                .thenAnswer(i -> configured.stream().map(Input::getId).collect(Collectors.toSet()));
        when(inputService.findByIds(anySet()))
                .thenAnswer(i -> configured.stream()
                        .filter(in -> ((Collection<String>) i.getArgument(0)).contains(in.getId()))
                        .collect(Collectors.toSet()));

        resource = new InputStatesResource(inputRegistry, mock(EventBus.class), inputService, messageInputFactory,
                inputStateService, inputLauncher, NODE_ID);
    }

    @Test
    void listReportsConfiguredInputWithoutRegistryStateAsStopped() throws Exception {
        configure(STOPPED_ID);

        assertThat(resource.list().states())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.id()).isEqualTo(STOPPED_ID);
                    assertThat(summary.state()).isEqualTo(IOState.Type.STOPPED.toString());
                    assertThat(summary.startedAt()).isEqualTo(CREATED_AT);
                });
    }

    @Test
    void listFetchesOnlyTheInputsMissingFromTheRegistry() throws Exception {
        final MessageInput running = configure(RUNNING_ID);
        configure(STOPPED_ID);
        final IOState<MessageInput> state = runningState(running);
        when(inputRegistry.stream()).thenAnswer(i -> Stream.of(state));

        assertThat(resource.list().states())
                .extracting(InputStateSummary::id, InputStateSummary::state)
                .containsExactlyInAnyOrder(
                        tuple(RUNNING_ID, IOState.Type.RUNNING.toString()),
                        tuple(STOPPED_ID, IOState.Type.STOPPED.toString()));

        verify(inputService).findByIds(Set.of(STOPPED_ID));
    }

    @Test
    void listDoesNotFetchDocumentsWhenEveryConfiguredInputIsRegistered() throws Exception {
        final MessageInput messageInput = configure(RUNNING_ID);
        final IOState<MessageInput> state = runningState(messageInput);
        when(inputRegistry.stream()).thenAnswer(i -> Stream.of(state));

        resource.list();

        verify(inputService, never()).findByIds(anySet());
    }

    @Test
    void listOmitsLeaderOnlyInputsOnNodesThatMustNotRunThem() throws Exception {
        final MessageInput messageInput = configure(STOPPED_ID);
        when(inputLauncher.leaderStatusInhibitsLaunch(messageInput)).thenReturn(true);

        assertThat(resource.list().states()).isEmpty();

        verify(inputLauncher).leaderStatusInhibitsLaunch(messageInput);
    }

    @Test
    void listOmitsConfiguredInputsWhileTheNodeIsStillLaunchingThem() throws Exception {
        configure(STOPPED_ID);
        when(inputLauncher.allPersistedLaunched()).thenReturn(false);

        assertThat(resource.list().states()).isEmpty();

        verify(inputService, never()).findIdsForThisNodeOrGlobal(anyString());
    }

    @Test
    void listFallsBackToRegistryStateWhenTheInputLookupFails() throws Exception {
        final MessageInput running = configure(RUNNING_ID);
        configure(STOPPED_ID);
        final IOState<MessageInput> state = runningState(running);
        when(inputRegistry.stream()).thenAnswer(i -> Stream.of(state));
        when(inputService.findIdsForThisNodeOrGlobal(NODE_ID.getNodeId()))
                .thenThrow(new IllegalStateException("MongoDB is unreachable"));

        assertThat(resource.list().states())
                .extracting(InputStateSummary::id)
                .containsExactly(RUNNING_ID);
    }

    @Test
    void listSkipsAnInputThatCannotBeInstantiatedAndReportsTheRest() throws Exception {
        configure(STOPPED_ID);
        configureUninstantiable(BROKEN_ID);

        assertThat(resource.list().states())
                .extracting(InputStateSummary::id)
                .containsExactly(STOPPED_ID);
    }

    @Test
    void getReturnsTheRegistryStateForARunningInput() throws Exception {
        final MessageInput messageInput = configure(RUNNING_ID);
        final IOState<MessageInput> state = runningState(messageInput);
        when(inputRegistry.getInputState(RUNNING_ID)).thenReturn(state);

        final InputStateSummary summary = resource.get(RUNNING_ID);

        assertThat(summary.id()).isEqualTo(RUNNING_ID);
        assertThat(summary.state()).isEqualTo(IOState.Type.RUNNING.toString());
        assertThat(summary.startedAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void getFailsForAnInputWithoutRegistryStateOnThisNode() throws Exception {
        configure(STOPPED_ID);

        assertThatThrownBy(() -> resource.get(STOPPED_ID)).isInstanceOf(NotFoundException.class);
    }

    private IOState<MessageInput> runningState(MessageInput input) {
        @SuppressWarnings("unchecked")
        final IOState<MessageInput> state = mock(IOState.class);
        when(state.getStoppable()).thenReturn(input);
        when(state.getState()).thenReturn(IOState.Type.RUNNING);
        when(state.getStartedAt()).thenReturn(CREATED_AT);
        return state;
    }

    private MessageInput configure(String id) throws Exception {
        final Input input = mock(Input.class);
        when(input.getId()).thenReturn(id);

        final MessageInput messageInput = mock(MessageInput.class);
        when(messageInput.getId()).thenReturn(id);
        when(messageInput.getTitle()).thenReturn("title-" + id);
        when(messageInput.getName()).thenReturn("Fake Input");
        when(messageInput.getType()).thenReturn("org.graylog2.inputs.FakeInput");
        when(messageInput.getCreatorUserId()).thenReturn("admin");
        when(messageInput.getCreatedAt()).thenReturn(CREATED_AT);
        when(messageInput.getConfiguration()).thenReturn(new Configuration(Map.of()));
        when(messageInput.getStaticFields()).thenReturn(Map.of());

        when(inputService.getMessageInput(input)).thenReturn(messageInput);
        configured.add(input);

        return messageInput;
    }

    private void configureUninstantiable(String id) throws Exception {
        final Input input = mock(Input.class);
        when(input.getId()).thenReturn(id);
        when(inputService.getMessageInput(input)).thenThrow(new IllegalArgumentException("broken configuration"));
        configured.add(input);
    }
}
