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
package org.graylog.plugins.views.storage.migration.state;

import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineContext;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;

import java.util.Optional;

public class InMemoryStateMachinePersistence implements DatanodeMigrationPersistence {

    private MigrationState currentState = MigrationState.NEW;
    private MigrationStateMachineContext context = MigrationStateMachineContext.create();

    @Override
    public Optional<DatanodeMigrationConfiguration> getConfiguration() {
        return Optional.of(new DatanodeMigrationConfiguration(currentState));
    }

    @Override
    public void saveConfiguration(DatanodeMigrationConfiguration configuration) {
        this.currentState = configuration.currentState();
    }

    @Override
    public Optional<MigrationStateMachineContext> getStateMachineContext() {
        return Optional.of(context);
    }

    @Override
    public void saveStateMachineContext(MigrationStateMachineContext context) {
        this.context = context;
    }
}
