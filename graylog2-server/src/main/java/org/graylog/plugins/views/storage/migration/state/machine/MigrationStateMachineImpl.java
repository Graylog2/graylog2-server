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
import com.github.oxo42.stateless4j.triggers.TriggerWithParameters1;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionContext;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.rest.CurrentStateInformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MigrationStateMachineImpl implements MigrationStateMachine {
    private final StateMachine<MigrationState, MigrationStep> stateMachine;
    private final MigrationActions migrationActions;

    public MigrationStateMachineImpl(StateMachine<MigrationState, MigrationStep> stateMachine, MigrationActions migrationActions) {
        this.stateMachine = stateMachine;
        this.migrationActions = migrationActions;
    }

    @Override
    public MigrationActionContext triggerWithContext(MigrationStep step, Map<String, Object> args) {
        MigrationState currentState = stateMachine.getState();
        MigrationActionContext context = new MigrationActionContext(args, currentState, step);
        TriggerWithParameters1<MigrationActionContext, MigrationStep> trigger =
                new TriggerWithParameters1<>(step, MigrationActionContext.class);
        try {
            stateMachine.fire(trigger, context);
        } catch (Exception e) {
            String message = (Objects.nonNull(e.getMessage())) ? e.getMessage() : e.getClass().getName();
            context.addError(message);
        }
        context.setResultState(new CurrentStateInformation(stateMachine.getState(), stateMachine.getPermittedTriggers(), context.getErrors()));
        return context;
    }


    @Override
    public MigrationState trigger(MigrationStep step, Map<String, Object> args) {
        try {
            migrationActions.setArgs(args);
            stateMachine.fire(step);
        } finally {
            migrationActions.clearArgs();
        }
        return stateMachine.getState();
    }

    @Override
    public MigrationState getState() {
        return stateMachine.getState();
    }

    @Override
    public List<MigrationStep> nextSteps() {
        return stateMachine.getPermittedTriggers();
    }

    @Override
    public String serialize() {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            stateMachine.configuration().generateDotFileInto(os, true);
            return os.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize state map", e);
        }
    }
}
