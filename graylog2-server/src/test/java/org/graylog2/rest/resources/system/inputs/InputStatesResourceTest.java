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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anySet;
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
        when(inputRegistry.stream()).thenReturn(Stream.empty());
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
    void listReportsRegistryStateOnlyOnceForARunningInput() throws Exception {
        final MessageInput messageInput = configure(RUNNING_ID);
        final IOState<MessageInput> state = runningState(messageInput);
        when(inputRegistry.stream()).thenReturn(Stream.of(state));

        assertThat(resource.list().states())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.id()).isEqualTo(RUNNING_ID);
                    assertThat(summary.state()).isEqualTo(IOState.Type.RUNNING.toString());
                });
    }

    @Test
    void listDoesNotFetchDocumentsWhenEveryConfiguredInputIsRegistered() throws Exception {
        final MessageInput messageInput = configure(RUNNING_ID);
        final IOState<MessageInput> state = runningState(messageInput);
        when(inputRegistry.stream()).thenReturn(Stream.of(state));

        resource.list();

        verify(inputService, never()).findByIds(anySet());
    }

    @Test
    void listOmitsLeaderOnlyInputsOnNodesThatMustNotRunThem() throws Exception {
        final MessageInput messageInput = configure(STOPPED_ID);
        when(inputLauncher.leaderStatusInhibitsLaunch(messageInput)).thenReturn(true);

        assertThat(resource.list().states()).isEmpty();
    }

    @Test
    void getReportsConfiguredInputWithoutRegistryStateAsStopped() throws Exception {
        configure(STOPPED_ID);

        final InputStateSummary summary = resource.get(STOPPED_ID);

        assertThat(summary.id()).isEqualTo(STOPPED_ID);
        assertThat(summary.state()).isEqualTo(IOState.Type.STOPPED.toString());
    }

    @Test
    void getOmitsLeaderOnlyInputsOnNodesThatMustNotRunThem() throws Exception {
        final MessageInput messageInput = configure(STOPPED_ID);
        when(inputLauncher.leaderStatusInhibitsLaunch(messageInput)).thenReturn(true);

        assertThatThrownBy(() -> resource.get(STOPPED_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFailsForAnInputThatIsNotConfiguredOnThisNode() throws Exception {
        when(inputService.findForThisNodeOrGlobal(NODE_ID.getNodeId(), STOPPED_ID))
                .thenThrow(new org.graylog2.database.NotFoundException("nope"));

        assertThatThrownBy(() -> resource.get(STOPPED_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFailsForAnUnknownInputIdThatTheLookupReportsAsNull() throws Exception {
        when(inputService.findForThisNodeOrGlobal(NODE_ID.getNodeId(), STOPPED_ID)).thenReturn(null);

        assertThatThrownBy(() -> resource.get(STOPPED_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getFailsForAMalformedInputId() throws Exception {
        when(inputService.findForThisNodeOrGlobal(NODE_ID.getNodeId(), "not-an-object-id"))
                .thenThrow(new IllegalArgumentException("invalid ObjectId"));

        assertThatThrownBy(() -> resource.get("not-an-object-id")).isInstanceOf(NotFoundException.class);
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
        when(inputService.findForThisNodeOrGlobal(NODE_ID.getNodeId(), id)).thenReturn(input);
        configured.add(input);

        return messageInput;
    }
}
