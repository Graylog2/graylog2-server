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
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;
import org.graylog.plugins.views.storage.migration.state.rest.CurrentStateInformation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MigrationStateMachineImpl implements MigrationStateMachine {
    private final StateMachine<MigrationState, MigrationStep> stateMachine;
    private final DatanodeMigrationPersistence persistenceService;
    private final MigrationStateMachineContext context;

    public MigrationStateMachineImpl(
            StateMachine<MigrationState, MigrationStep> stateMachine,
            DatanodeMigrationPersistence persistenceService,
            MigrationStateMachineContext context) {
        this.stateMachine = stateMachine;
        this.persistenceService = persistenceService;
        this.context = context;
    }

    @Override
    public CurrentStateInformation trigger(MigrationStep step, Map<String, Object> args) {
        context.setCurrentStep(step);
        if (Objects.nonNull(args) && !args.isEmpty()) {
            context.addActionArguments(step, args);
        }
        String errorMessage = null;
        try {
            stateMachine.fire(step);
        } catch (Exception e) {
            errorMessage = Objects.nonNull(e.getMessage()) ? e.getMessage() : e.toString();
        }
        saveContext();
        return new CurrentStateInformation(getState(), nextSteps(), errorMessage, context.getResponse());
    }

    @Override
    public void saveContext() {
        persistenceService.saveStateMachineContext(context);
    }

    @Override
    public MigrationStateMachineContext getContext() {
        return context;
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


    /**
     * The state machine is configured to obtain from and persist state via the underlying persistence service. If we
     * change the state in the persistence, it will be automatically changed in the state machine as well.
     *
     * @see MigrationStateMachineBuilder#buildFromPersistedState(DatanodeMigrationPersistence, MigrationActions)
     */
    @Override
    public void reset() {
        persistenceService.saveStateMachineContext(new MigrationStateMachineContext());
        persistenceService.saveConfiguration(new DatanodeMigrationConfiguration(MigrationState.NEW));
    }
}
