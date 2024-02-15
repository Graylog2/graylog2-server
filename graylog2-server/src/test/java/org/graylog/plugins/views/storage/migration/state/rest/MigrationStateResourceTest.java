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
package org.graylog.plugins.views.storage.migration.state.rest;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MigrationStateResourceTest {

    public static final String AUTHORIZATION = "MyAuthorization";
    @Mock
    MigrationStateMachine stateMachine;
    @Mock
    HttpHeaders httpHeaders;
    MigrationStateMachineContext stateMachineContext;
    MigrationStateResource migrationStateResource;

    @BeforeEach
    public void setUp() {
        stateMachineContext = new MigrationStateMachineContext();
        when(stateMachine.getContext()).thenReturn(stateMachineContext);
        when(httpHeaders.getRequestHeader(HttpHeaders.AUTHORIZATION)).thenReturn(List.of(AUTHORIZATION));
        migrationStateResource = new MigrationStateResource(stateMachine, httpHeaders);
    }

    @Test
    public void authenticationTokenSetToStateMachineContext() {
        assertThat(stateMachineContext.getExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY)).isEqualTo(AUTHORIZATION);
    }

    @Test
    public void requestReturnsSuccessfulResult() {
        CurrentStateInformation state = new CurrentStateInformation(MigrationState.NEW, List.of(MigrationStep.SELECT_MIGRATION));
        when(stateMachine.trigger(any(), anyMap())).thenReturn(state);
        try (Response response = migrationStateResource.migrate(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isEqualTo(state);
        }
    }

    @Test
    public void requestReturns500OnError() {
        CurrentStateInformation state =
                new CurrentStateInformation(MigrationState.NEW, List.of(MigrationStep.SELECT_MIGRATION), "Error");
        when(stateMachine.trigger(any(), anyMap())).thenReturn(state);
        try (Response response = migrationStateResource.migrate(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isEqualTo(state);
        }
    }


}
