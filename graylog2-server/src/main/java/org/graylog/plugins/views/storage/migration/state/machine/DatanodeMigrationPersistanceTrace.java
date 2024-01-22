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

import com.github.oxo42.stateless4j.delegates.Trace;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;

/**
 * This is a {@link Trace} that gets notified every time a state machine is transitioning between two states.
 * We use this information to persist the state in the database.
 */
public class DatanodeMigrationPersistanceTrace implements Trace<MigrationState, MigrationStep> {

    private final DatanodeMigrationPersistence delegate;

    public DatanodeMigrationPersistanceTrace(DatanodeMigrationPersistence persistenceService) {
        this.delegate = persistenceService;
    }

    @Override
    public void trigger(MigrationStep trigger) {
        // do nothing, ignored
    }

    @Override
    public void transition(MigrationStep trigger, MigrationState source, MigrationState destination) {
        // persist each transition destination as the current state in the database
        delegate.saveConfiguration(new DatanodeMigrationConfiguration(destination));
    }
}
