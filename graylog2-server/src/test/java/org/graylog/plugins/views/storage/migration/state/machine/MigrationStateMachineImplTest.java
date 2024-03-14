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
import org.graylog.plugins.views.storage.migration.state.InMemoryStateMachinePersistence;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionsImpl;
import org.graylog.plugins.views.storage.migration.state.rest.CurrentStateInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;


class MigrationStateMachineImplTest {

    MigrationState INITIAL_STATE = MigrationState.NEW;
    MigrationState RESULT_STATE = MigrationState.MIGRATION_WELCOME_PAGE;
    MigrationStep MIGRATION_STEP = MigrationStep.SELECT_MIGRATION;

    MigrationActions migrationActions;
    InMemoryStateMachinePersistence persistenceService = new InMemoryStateMachinePersistence();
    MigrationStateMachine migrationStateMachine;

    @BeforeEach
    public void setUp() {
        migrationActions = new TestMigrationActions();
    }

    @Test
    public void smReturnsResultState() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {});
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of());
        assertThat(context.hasErrors()).isFalse();
        assertThat(context.state()).isEqualTo(RESULT_STATE);
    }

    @Test
    public void smPassesArgumentsToAction() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            assertThat(context.getActionArgument("arg1", String.class)).isEqualTo("v1");
            assertThat(context.getActionArgument("arg2", Integer.class)).isEqualTo(2);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of(
                "arg1", "v1", "arg2", 2
        ));
        assertThat(context.hasErrors()).isFalse();
    }

    @Test
    public void smActionCanSetExtendedState() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            int i = context.getActionArgument("k1", Integer.class);
            context.addExtendedState("r1", ++i);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of("k1", 1));
        assertThat(context.hasErrors()).isFalse();
        assertThat(migrationStateMachine.getContext().getExtendedState("r1")).isEqualTo(2);
    }

    @Test
    public void smThrowsErrorOnNoArguments() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            context.getActionArgument("away", String.class);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, null);
        assertThat(context.hasErrors()).isTrue();
        assertThat(context.errorMessage()).isEqualTo("Missing arguments for step SELECT_MIGRATION");
    }

    @Test
    public void smThrowsErrorOnMissingArgument() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            context.getActionArgument("away", String.class);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of("k1", "v1"));
        assertThat(context.hasErrors()).isTrue();
        assertThat(context.errorMessage()).isEqualTo("Missing argument away for step SELECT_MIGRATION");
    }

    @Test
    public void smThrowsErrorOnWrongArgumentType() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            context.getActionArgument("k1", Integer.class);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of("k1", "v1"));
        assertThat(context.hasErrors()).isTrue();
        assertThat(context.errorMessage()).isEqualTo("Argument k1 must be of type class java.lang.Integer");
    }

    @Test
    public void smSetsErrorOnExceptionInAction() {
        String errorMessage = "Error 40: Insufficient Coffee.";
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            throw new RuntimeException(errorMessage);
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of());
        assertThat(context.hasErrors()).isTrue();
        assertThat(context.errorMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void smStateUnchangedOnExceptionInAction() {
        StateMachine<MigrationState, MigrationStep> stateMachine = testStateMachineWithAction((context) -> {
            throw new RuntimeException();
        });
        migrationStateMachine = new MigrationStateMachineImpl(stateMachine, migrationActions, persistenceService);
        CurrentStateInformation context = migrationStateMachine.trigger(MIGRATION_STEP, Map.of());
        assertThat(context.hasErrors()).isTrue();
        assertThat(context.state()).isEqualTo(INITIAL_STATE);
    }

    private StateMachine<MigrationState, MigrationStep> testStateMachineWithAction(Consumer<MigrationStateMachineContext> action) {
        StateMachineConfig<MigrationState, MigrationStep> config = new StateMachineConfig<>();
        config.configure(INITIAL_STATE)
                .permit(MIGRATION_STEP, RESULT_STATE, () -> {
                    TestMigrationActions testMigrationActions = (TestMigrationActions) migrationActions;
                    testMigrationActions.runTestFunction(action);
                });
        return new StateMachine<>(INITIAL_STATE, config);
    }


    private static class TestMigrationActions extends MigrationActionsAdapter {

        public void runTestFunction(Consumer<MigrationStateMachineContext> testFunction) {
            testFunction.accept(getStateMachineContext());
        }

    }


}
