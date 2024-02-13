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
import com.google.common.annotations.VisibleForTesting;
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

        // All actions which can fail should be performed on transition to make sure that there is no state change on error.
        // For async tasks, the task should be triggered in the transition with a following intermediary step which
        // has a guard to continue only on task completion.

        StateMachineConfig<MigrationState, MigrationStep> config = new StateMachineConfig<>();

        config.configure(MigrationState.NEW)
                .permit(MigrationStep.SELECT_MIGRATION, MigrationState.MIGRATION_WELCOME_PAGE, () -> LOG.info("Migration selected in menu, show welcome page"));

        config.configure(MigrationState.MIGRATION_WELCOME_PAGE)
                .permit(MigrationStep.SHOW_DIRECTORY_COMPATIBILITY_CHECK, MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE, () -> LOG.info("Showing directory compatibility check page"))
                .permit(MigrationStep.SKIP_DIRECTORY_COMPATIBILITY_CHECK, MigrationState.CA_CREATION_PAGE);

        config.configure(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .onEntry(migrationActions::runDirectoryCompatibilityCheck)
                .permitIf(MigrationStep.SHOW_CA_CREATION, MigrationState.CA_CREATION_PAGE, migrationActions::directoryCompatibilityCheckOk);

        config.configure(MigrationState.CA_CREATION_PAGE)
                .permitIf(MigrationStep.SHOW_RENEWAL_POLICY_CREATION, MigrationState.RENEWAL_POLICY_CREATION_PAGE, () -> !migrationActions.caDoesNotExist())
                .permitIf(MigrationStep.SHOW_MIGRATION_SELECTION, MigrationState.MIGRATION_SELECTION_PAGE, migrationActions::caAndRemovalPolicyExist);

        config.configure(MigrationState.RENEWAL_POLICY_CREATION_PAGE)
                .permitIf(MigrationStep.SHOW_MIGRATION_SELECTION, MigrationState.MIGRATION_SELECTION_PAGE, migrationActions::caAndRemovalPolicyExist);

        // Major decision - remote reindexing or rolling upgrade(in-place)?
        config.configure(MigrationState.MIGRATION_SELECTION_PAGE)
                .permit(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION, MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .permit(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION, MigrationState.REMOTE_REINDEX_WELCOME_PAGE);

        // remote reindexing branch of the migration
        config.configure(MigrationState.REMOTE_REINDEX_WELCOME_PAGE)
                .onEntry(migrationActions::reindexUpgradeSelected)
                .permit(MigrationStep.DISCOVER_NEW_DATANODES, MigrationState.PROVISION_DATANODE_CERTIFICATES_PAGE, () -> {
                    LOG.info("Remote Reindexing selected");
                });

        config.configure(MigrationState.PROVISION_DATANODE_CERTIFICATES_PAGE)
                .permit(MigrationStep.PROVISION_DATANODE_CERTIFICATES, MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING, migrationActions::provisionDataNodes);

        config.configure(MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING)
                .permitIf(MigrationStep.SHOW_DATA_MIGRATION_QUESTION, MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE, migrationActions::provisioningFinished);

        config.configure(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE)
                .permit(MigrationStep.SHOW_MIGRATE_EXISTING_DATA, MigrationState.MIGRATE_EXISTING_DATA)
                .permit(MigrationStep.SKIP_EXISTING_DATA_MIGRATION, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);

        config.configure(MigrationState.MIGRATE_EXISTING_DATA)
                .onEntry(migrationActions::reindexOldData)
                .permitIf(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER, migrationActions::reindexingFinished);

        // in place / rolling upgrade branch of the migration
        config.configure(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .onEntry(migrationActions::rollingUpgradeSelected)
                .permit(MigrationStep.INSTALL_DATANODES_ON_EVERY_NODE, MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE2, () -> LOG.info("Showing directory compatibility check page"));

        config.configure(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE2)
                .permitIf(MigrationStep.SHOW_PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES, MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES, migrationActions::directoryCompatibilityCheckOk);

        config.configure(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES)
                .permit(MigrationStep.PROVISION_DATANODE_CERTIFICATES, MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING, migrationActions::provisionDataNodes);

        config.configure(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .permitIf(MigrationStep.CALCULATE_JOURNAL_SIZE, MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING, migrationActions::provisioningFinished);

        config.configure(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .permit(MigrationStep.SHOW_STOP_PROCESSING_PAGE, MigrationState.MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART);

        config.configure(MigrationState.MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART)
                .onEntry(migrationActions::stopMessageProcessing)
                .onExit(migrationActions::startMessageProcessing)
                .permit(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);

        // common cleanup steps
        config.configure(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .permitIf(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED, MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG, migrationActions::isOldClusterStopped);

        config.configure(MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG)
                .permit(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED, MigrationState.FINISHED);

        return config;
    }

    @VisibleForTesting
    static StateMachine<MigrationState, MigrationStep> buildWithTestState(MigrationState state, MigrationActions migrationActions) {
        final StateMachineConfig<MigrationState, MigrationStep> config = configureStates(migrationActions);
        return new StateMachine<>(state, config);
    }

    private static StateMachine<MigrationState, MigrationStep> fromState(MigrationState state, Trace<MigrationState, MigrationStep> persistence, MigrationActions migrationActions) {
        final StateMachineConfig<MigrationState, MigrationStep> config = configureStates(migrationActions);
        final StateMachine<MigrationState, MigrationStep> stateMachine = new StateMachine<>(state, config);
        stateMachine.fireInitialTransition(); // asserts that onEntry will be performed when loading persisted state
        stateMachine.setTrace(persistence);
        return stateMachine;
    }

    public static StateMachine<MigrationState, MigrationStep> buildFromPersistedState(DatanodeMigrationPersistence persistenceService, MigrationActions migrationActions) {
        final MigrationState state = persistenceService.getConfiguration().map(DatanodeMigrationConfiguration::currentState).orElse(MigrationState.NEW);
        final Trace<MigrationState, MigrationStep> persitanceTrace = new DatanodeMigrationPersistanceTrace(persistenceService);
        return fromState(state, persitanceTrace, migrationActions);
    }
}
