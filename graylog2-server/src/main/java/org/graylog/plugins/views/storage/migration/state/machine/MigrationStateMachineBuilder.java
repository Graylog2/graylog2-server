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
import com.github.oxo42.stateless4j.delegates.Trace;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationStateMachineBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationStateMachineBuilder.class);

    @NotNull
    private static StateMachineConfig<MigrationState, MigrationStep> configureStates(MigrationActions migrationActions) {
        StateMachineConfig<MigrationState, MigrationStep> config = new StateMachineConfig<>();

        // Major decision - remote reindexing or rolling upgrade(in-place)?
        config.configure(MigrationState.NEW)
                .permit(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION, MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME, migrationActions::rollingUpgradeSelected)
                .permit(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION, MigrationState.REMOTE_REINDEX_WELCOME, () -> {
                    LOG.info("Selected remote reindex migration");
                });


        // remote reindexing branch of the migration
        config.configure(MigrationState.REMOTE_REINDEX_WELCOME)
                .permit(MigrationStep.DISCOVER_NEW_DATANODES, MigrationState.PROVISION_DATANODE_CERTIFICATES, () -> {
                    LOG.info("Compatibility check succeeded");
                });

        config.configure(MigrationState.PROVISION_DATANODE_CERTIFICATES)
                .permit(MigrationStep.MIGRATE_INDEX_TEMPLATES, MigrationState.EXISTING_DATA_MIGRATION_QUESTION, migrationActions::migrateIndexTemplates);

        config.configure(MigrationState.EXISTING_DATA_MIGRATION_QUESTION)
                .permit(MigrationStep.MIGRATE_EXISTING_DATA, MigrationState.MIGRATE_WITH_DOWNTIME_QUESTION)
                .permit(MigrationStep.SKIP_EXISTING_DATA_MIGRATION, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);

        config.configure(MigrationState.MIGRATE_WITH_DOWNTIME_QUESTION)
                .permit(MigrationStep.MIGRATE_CLUSTER_WITH_DOWNTIME, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER, migrationActions::migrateWithDowntime)
                .permit(MigrationStep.MIGRATE_CLUSTER_WITHOUT_DOWNTIME, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER, migrationActions::migrateWithoutDowntime);

        config.configure(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .permitIf(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED, MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG, migrationActions::isOldClusterStopped);


        // inplace / rolling upgrade branch of the migration
        config.configure(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME)
                .permit(MigrationStep.INSTALL_DATANODES_ON_EVERY_NODE, MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE);

        config.configure(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .permit(MigrationStep.DIRECTORY_COMPATIBILITY_CHECK_OK, MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES);


        config.configure(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES)
                .permit(MigrationStep.CALCULATE_JOURNAL_SIZE, MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);

        config.configure(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .permit(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED, MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG);

        // common cleanup steps
        config.configure(MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG)
                .permit(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED, MigrationState.FINISHED);

        return config;
    }

    private static StateMachine<MigrationState, MigrationStep> fromState(MigrationState state, Trace<MigrationState, MigrationStep> persistence, MigrationActions migrationActions) {
        final StateMachineConfig<MigrationState, MigrationStep> config = configureStates(migrationActions);
        final StateMachine<MigrationState, MigrationStep> stateMachine = new StateMachine<>(state, config);
        stateMachine.setTrace(persistence);
        return stateMachine;
    }

    public static StateMachine<MigrationState, MigrationStep> buildFromPersistedState(DatanodeMigrationPersistence persistenceService, MigrationActions migrationActions) {
        final MigrationState state = persistenceService.getConfiguration().map(DatanodeMigrationConfiguration::currentState).orElse(MigrationState.NEW);
        final Trace<MigrationState, MigrationStep> persitanceTrace = new DatanodeMigrationPersistanceTrace(persistenceService);
        return fromState(state, persitanceTrace, migrationActions);
    }
}
