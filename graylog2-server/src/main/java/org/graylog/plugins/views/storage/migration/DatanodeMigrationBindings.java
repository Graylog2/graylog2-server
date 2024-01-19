package org.graylog.plugins.views.storage.migration;

import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActionsImpl;
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
        bind(MigrationStateMachine.class).toProvider(MigrationStateMachineProvider.class);
        bind(DatanodeMigrationPersistence.class).to(DatanodeMigrationConfigurationImpl.class);
        bind(MigrationActions.class).to(MigrationActionsImpl.class);
    }
}
