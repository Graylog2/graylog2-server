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
import org.apache.commons.lang3.tuple.Pair;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

public class MigrationStateResourceTest {

    @Test
    public void authenticationTokenSetToStateMachineContext() {
        final CurrentStateInformation state = new CurrentStateInformation(MigrationState.NEW, List.of(MigrationStep.SELECT_MIGRATION));
        final MigrationStateMachine stateMachine = mockStateMachine(state);

        final String expectedAuthToken = "MyAuthorization";
        final MigrationStateResource resource = new MigrationStateResource(
                stateMachine,
                mockHttpHeaders(Pair.of(HttpHeaders.AUTHORIZATION, expectedAuthToken))
        );

        assertThat(stateMachine.getContext().getExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY)).isEqualTo(expectedAuthToken);
    }

    @Test
    public void requestReturnsSuccessfulResult() {
        CurrentStateInformation state = new CurrentStateInformation(MigrationState.NEW, List.of(MigrationStep.SELECT_MIGRATION));
        final MigrationStateResource resource = new MigrationStateResource(mockStateMachine(state), mockHttpHeaders());

        try (Response response = resource.trigger(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity()).isEqualTo(state);
        }
    }

    @Test
    public void requestReturns500OnError() {
        final CurrentStateInformation expectedState = new CurrentStateInformation(MigrationState.NEW, List.of(MigrationStep.SELECT_MIGRATION), "Error", null);
        final MigrationStateResource resource = new MigrationStateResource(mockStateMachine(expectedState), mockHttpHeaders());

        try (Response response = resource.trigger(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(500);
            assertThat(response.getEntity()).isEqualTo(expectedState);
        }
    }

    @SafeVarargs
    private HttpHeaders mockHttpHeaders(Pair<String, String>... headers) {
        final HttpHeaders httpHeaders = Mockito.mock(HttpHeaders.class);
        Arrays.stream(headers).forEach(pair -> when(httpHeaders.getRequestHeader(pair.getKey())).thenReturn(List.of(pair.getValue())));
        return httpHeaders;
    }

    private MigrationStateMachine mockStateMachine(CurrentStateInformation state) {

        final MigrationStateMachineContext context = new MigrationStateMachineContext();

        return new MigrationStateMachine() {
            @Override
            public CurrentStateInformation trigger(MigrationStep step, Map<String, Object> args) {
                return state;
            }

            @Override
            public MigrationState getState() {
                return state.state();
            }

            @Override
            public List<MigrationStep> nextSteps() {
                return state.nextSteps();
            }

            @Override
            public MigrationStateMachineContext getContext() {
                return context;
            }

            @Override
            public String serialize() {
                throw new UnsupportedOperationException("not implemented");
            }
        };
    }
}
