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
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionsFactory;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;

@Singleton
public class MigrationStateMachineProvider implements Provider<MigrationStateMachine> {

    private final DatanodeMigrationPersistence persistenceService;
    private final MigrationActionsFactory migrationActionsFactory;

    @Inject
    public MigrationStateMachineProvider(DatanodeMigrationPersistence persistenceService, MigrationActionsFactory migrationActionsFactory) {
       this.persistenceService = persistenceService;
       this.migrationActionsFactory = migrationActionsFactory;
    }

    @Override
    public MigrationStateMachine get() {
        final MigrationStateMachineContext context = persistenceService.getStateMachineContext().orElseGet(MigrationStateMachineContext::new);
        final MigrationActions migrationActions = migrationActionsFactory.create(context);
        final StateMachine<MigrationState, MigrationStep> stateMachine = MigrationStateMachineBuilder.buildFromPersistedState(persistenceService, migrationActions);
        return new MigrationStateMachineImpl(stateMachine, persistenceService, context);
    }
}
