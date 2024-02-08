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
package org.graylog.plugins.views.storage.migration.state.machine;

import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationAction;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionContext;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class MigrationStateMachineImplTest {

    MigrationState INITIAL_STATE = MigrationState.NEW;
    MigrationState RESULT_STATE = MigrationState.MIGRATION_WELCOME_PAGE;
    MigrationStep MIGRATION_STEP = MigrationStep.SELECT_MIGRATION;

    @Mock
    MigrationActions migrationActions; //todo: could be removed if context can be used
    MigrationStateMachine migrationStateMachine;

    @Test
    public void contextFieldsSet() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {});
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        Map<String, Object> args = Map.of("k1", "v1");
        MigrationActionContext context = migrationStateMachine.triggerWithContext(MIGRATION_STEP, args);
        assertEquals(INITIAL_STATE, context.getPreviousState());
        assertEquals(MIGRATION_STEP, context.getRequestedStep());
        assertEquals(args, context.getArgs());
    }

    @Test
    public void smReturnsResultState() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {});
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        MigrationActionContext context = migrationStateMachine.triggerWithContext(MIGRATION_STEP, Map.of());
        assertFalse(context.hasErrors());
        assertEquals(RESULT_STATE, context.getResultState().state());
    }

    @Test
    public void smPassesArgumentsToAction() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            assertEquals("v1", context.getArgs().get("arg1"));
            assertEquals(2, context.getArgs().get("arg2"));
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        MigrationActionContext context = migrationStateMachine.triggerWithContext(MIGRATION_STEP, Map.of(
                "arg1", "v1", "arg2", 2
        ));
    }

    @Test
    public void smCallsActionOnTransition() {
        MigrationAction action = mock(MigrationAction.class);
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction(action);
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        verifyNoInteractions(action);
        migrationStateMachine.triggerWithContext(MIGRATION_STEP, Map.of());
        verify(action, times(1)).doIt(any());
    }

    @Test
    public void smSetsErrorOnExceptionInAction() {
        String errorMessage = "Error 40: Insufficient Coffee.";
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            throw new RuntimeException(errorMessage);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        MigrationActionContext context = migrationStateMachine.triggerWithContext(MIGRATION_STEP, Map.of());
        assertTrue(context.hasErrors());
        assertEquals(errorMessage, context.getErrors().get(0));
    }

    @Test
    public void smStateUnchangedOnExceptionInAction() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            throw new RuntimeException();
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions);
        MigrationActionContext context = migrationStateMachine.triggerWithContext(MIGRATION_STEP, Map.of());
        assertEquals(context.getPreviousState(), context.getResultState().state());
    }

    private StateMachine<MigrationState, MigrationStep> testStateMachineWithAction(MigrationAction action) {
        StateMachineConfig<MigrationState, MigrationStep> config = new StateMachineConfig<>();
        config.configure(INITIAL_STATE)
                .permitDynamic(createDynamicTrigger(MIGRATION_STEP), (context) -> RESULT_STATE, action);
        return new StateMachine<>(INITIAL_STATE, config);
    }

    private TriggerWithParameters1<MigrationActionContext, MigrationStep> createDynamicTrigger(MigrationStep step) {
        return new TriggerWithParameters1<>(step, MigrationActionContext.class);

    }


}
