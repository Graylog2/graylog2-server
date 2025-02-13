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
package org.graylog.plugins.views.storage.migration;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionsFactory;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionsImpl;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationShutdownService;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachine;
import org.graylog.plugins.views.storage.migration.state.machine.MigrationStateMachineProvider;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfigurationImpl;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;
import org.graylog.plugins.views.storage.migration.state.rest.MigrationStateResource;
import org.graylog2.plugin.inject.Graylog2Module;

public class DatanodeMigrationBindings extends Graylog2Module {
    @Override
    protected void configure() {
        addSystemRestResource(MigrationStateResource.class);
        bind(DatanodeMigrationPersistence.class).to(DatanodeMigrationConfigurationImpl.class);
        install(new FactoryModuleBuilder().implement(MigrationActions.class, MigrationActionsImpl.class).build(
                MigrationActionsFactory.class));
        bind(MigrationStateMachine.class).toProvider(MigrationStateMachineProvider.class);
        serviceBinder().addBinding().to(MigrationShutdownService.class).asEagerSingleton();
    }
}
