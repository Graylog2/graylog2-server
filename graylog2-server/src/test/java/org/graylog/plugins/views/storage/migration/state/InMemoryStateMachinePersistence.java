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

import com.github.oxo42.stateless4j.delegates.Trace;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationState;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStep;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;

import java.util.Optional;

public class InMemoryStateMachinePersistence implements DatanodeMigrationPersistence {

    private MigrationState currentState = MigrationState.NEW;

    @Override
    public Optional<DatanodeMigrationConfiguration> getConfiguration() {
        return Optional.of(new DatanodeMigrationConfiguration(currentState));
    }

    @Override
    public void saveConfiguration(DatanodeMigrationConfiguration configuration) {
        this.currentState = configuration.currentState();
    }
}
