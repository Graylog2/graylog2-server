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
import org.assertj.core.api.Assertions;

public class AssertableStateMachine {

    private final StateMachine<MigrationState, MigrationStep> stateMachine;
    private final TestableMigrationActions migrationActions;

    public AssertableStateMachine(StateMachine<MigrationState, MigrationStep> stateMachine, TestableMigrationActions migrationActions) {
        this.stateMachine = stateMachine;
        this.migrationActions = migrationActions;
    }

    public AssertableStateMachine assertState(MigrationState expectedState) {
        Assertions.assertThat(stateMachine.getState()).isEqualTo(expectedState);
        return this;
    }

    public AssertableStateMachine assertTransition(MigrationStep... expectedSteps) {
        Assertions.assertThat(stateMachine.getPermittedTriggers()).containsExactlyInAnyOrder(expectedSteps);
        return this;
    }

    public AssertableStateMachine assertEmptyTransitions() {
        Assertions.assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        return this;
    }

    public AssertableStateMachine fire(MigrationStep nextStep) {
        stateMachine.fire(nextStep);
        return this;
    }

    public AssertableStateMachine assertActionTriggered(TestableAction triggeredAction) {
        Assertions.assertThat(migrationActions.getTriggeredActions()).contains(triggeredAction);
        return this;
    }
}
