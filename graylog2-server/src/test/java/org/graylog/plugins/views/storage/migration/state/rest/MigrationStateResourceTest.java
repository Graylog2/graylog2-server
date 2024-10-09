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

import com.github.joschi.jadconfig.util.Size;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.graylog.plugins.views.storage.migration.state.InMemoryStateMachinePersistence;
import org.graylog.plugins.views.storage.migration.state.actions.TrafficSnapshot;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationActionsAdapter;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineBuilder;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineImpl;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MigrationStateResourceTest {

    @Mock
    KafkaJournalConfiguration journalConfiguration;

    @Test
    public void authenticationTokenSetToStateMachineContext() {
        final MigrationStateMachine stateMachine = createStateMachine();

        final String expectedAuthToken = "MyAuthorization";
        new MigrationStateResource(
                stateMachine,
                mockHttpHeaders(Pair.of(HttpHeaders.AUTHORIZATION, expectedAuthToken)),
                journalConfiguration
        );

        assertThat(stateMachine.getContext().getExtendedState(MigrationStateMachineContext.AUTH_TOKEN_KEY)).isEqualTo(expectedAuthToken);
    }

    @Test
    public void requestReturnsSuccessfulResult() {
        final MigrationStateResource resource = new MigrationStateResource(createStateMachine(), mockHttpHeaders(), journalConfiguration);

        try (Response response = resource.trigger(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getEntity())
                    .isInstanceOf(CurrentStateInformation.class)
                    .extracting(e -> (CurrentStateInformation) e)
                    .satisfies(entity -> assertThat(entity.hasErrors()).isFalse());
        }
    }

    @Test
    public void requestReturns500OnError() {
        final MigrationStateResource resource = new MigrationStateResource(createStateMachine(), mockHttpHeaders(), journalConfiguration);
        // trigger a step that's not allowed in this state. That should cause and propagate an error
        try (Response response = resource.trigger(new MigrationStepRequest(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED, Map.of()))) {
            assertThat(response.getStatus()).isEqualTo(500);
            final Object entity = response.getEntity();
            assertThat(entity)
                    .isInstanceOf(CurrentStateInformation.class)
                    .extracting(e -> (CurrentStateInformation) e)
                    .extracting(CurrentStateInformation::errorMessage)
                    .isEqualTo("No valid leaving transitions are permitted from state 'NEW' for trigger 'CONFIRM_OLD_CLUSTER_STOPPED'. Consider ignoring the trigger.");
        }
    }

    @Test
    void testReset() {
        final MigrationStateResource resource = new MigrationStateResource(createStateMachine(), mockHttpHeaders(), journalConfiguration);
        resource.trigger(new MigrationStepRequest(MigrationStep.SELECT_MIGRATION, Map.of()));
        assertThat(resource.status().state()).isEqualTo(MigrationState.MIGRATION_WELCOME_PAGE);
        resource.resetState();
        assertThat(resource.status().state()).isEqualTo(MigrationState.NEW);
    }

    @Test
    void testJournalEstimate() {
        MigrationStateMachine stateMachine = createStateMachine();
        final MigrationStateResource resource = new MigrationStateResource(stateMachine, mockHttpHeaders(), journalConfiguration);
        when(journalConfiguration.getMessageJournalMaxSize()).thenReturn(Size.bytes(10000));
        JournalEstimate journalEstimate = resource.getJournalEstimate();
        assertThat(journalEstimate.journalSize()).isEqualTo(10000L);
        assertThat(journalEstimate.maxDowntimeMinutes()).isEqualTo(10000L);
        stateMachine.getContext().addExtendedState(TrafficSnapshot.ESTIMATED_TRAFFIC_PER_MINUTE, 3000L);
        assertThat(resource.getJournalEstimate().maxDowntimeMinutes()).isEqualTo(3L);
    }

    @SafeVarargs
    private HttpHeaders mockHttpHeaders(Pair<String, String>... headers) {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        Arrays.stream(headers).forEach(pair -> when(httpHeaders.getRequestHeader(pair.getKey())).thenReturn(List.of(pair.getValue())));
        return httpHeaders;
    }

    private MigrationStateMachine createStateMachine() {
        final InMemoryStateMachinePersistence persistence = new InMemoryStateMachinePersistence();
        final MigrationStateMachineContext context = new MigrationStateMachineContext();
        final MigrationActionsAdapter actions = new MigrationActionsAdapter(context);
        return new MigrationStateMachineImpl(
                MigrationStateMachineBuilder.buildFromPersistedState(persistence, actions),
                persistence,
                context);
    }
}
