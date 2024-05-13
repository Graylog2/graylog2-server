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
import com.github.oxo42.stateless4j.delegates.Action1;
import com.github.oxo42.stateless4j.delegates.Func;
import com.google.common.annotations.VisibleForTesting;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationConfiguration;
import org.graylog.plugins.views.storage.migration.state.persistence.DatanodeMigrationPersistence;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationStateMachineBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationStateMachineBuilder.class);

    @Nonnull
    private static StateMachineConfig<MigrationState, MigrationStep> configureStates(MigrationActions migrationActions) {

        // All actions which can fail should be performed on transition to make sure that there is no state change on error.
        // For async tasks, the task should be triggered in the transition with a following intermediary step which
        // has a guard to continue only on task completion.

        StateMachineConfig<MigrationState, MigrationStep> config = new StateMachineConfig<>();

        config.configure(MigrationState.NEW)
                .permit(MigrationStep.SELECT_MIGRATION, MigrationState.MIGRATION_WELCOME_PAGE, () -> LOG.info("Migration selected in menu, show welcome page"));

        config.configure(MigrationState.MIGRATION_WELCOME_PAGE)
                .permitIf(MigrationStep.SHOW_CA_CREATION, MigrationState.CA_CREATION_PAGE, migrationActions::caDoesNotExist)
                .permitIf(MigrationStep.SHOW_RENEWAL_POLICY_CREATION, MigrationState.RENEWAL_POLICY_CREATION_PAGE, () -> !migrationActions.caDoesNotExist() && migrationActions.renewalPolicyDoesNotExist())
                .permitIf(MigrationStep.SHOW_MIGRATION_SELECTION, MigrationState.MIGRATION_SELECTION_PAGE, migrationActions::caAndRenewalPolicyExist);

        config.configure(MigrationState.CA_CREATION_PAGE)
                .permitIf(MigrationStep.SHOW_RENEWAL_POLICY_CREATION, MigrationState.RENEWAL_POLICY_CREATION_PAGE, () -> !migrationActions.caDoesNotExist() && migrationActions.renewalPolicyDoesNotExist())
                .permitIf(MigrationStep.SHOW_MIGRATION_SELECTION, MigrationState.MIGRATION_SELECTION_PAGE, migrationActions::caAndRenewalPolicyExist);

        config.configure(MigrationState.RENEWAL_POLICY_CREATION_PAGE)
                .permitIf(MigrationStep.SHOW_MIGRATION_SELECTION, MigrationState.MIGRATION_SELECTION_PAGE, migrationActions::caAndRenewalPolicyExist);

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
                .permitIf(MigrationStep.PROVISION_DATANODE_CERTIFICATES, MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING, () -> !migrationActions.dataNodeStartupFinished(), migrationActions::provisionAndStartDataNodes)
                .permitIf(MigrationStep.SHOW_DATA_MIGRATION_QUESTION, MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE, migrationActions::dataNodeStartupFinished);

        // This page should contain the "Please restart Graylog to continue with data migration"
        config.configure(MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING)
                .permitIf(MigrationStep.SHOW_DATA_MIGRATION_QUESTION, MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE, migrationActions::dataNodeStartupFinished);

        config.configure(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE)
                .permit(MigrationStep.SHOW_MIGRATE_EXISTING_DATA, MigrationState.MIGRATE_EXISTING_DATA)
                .permit(MigrationStep.SKIP_EXISTING_DATA_MIGRATION, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);

        // we now have enough information in the context to start the remote reindex migration. This will move us to the
        // next state that will be active as long as the migration is running and will provide status information to the FE
        config.configure(MigrationState.MIGRATE_EXISTING_DATA) // this state and screen has to request username, password and url of the old cluster
                .permitReentry(MigrationStep.CHECK_REMOTE_INDEXER_CONNECTION, migrationActions::verifyRemoteIndexerConnection)
                .permit(MigrationStep.START_REMOTE_REINDEX_MIGRATION, MigrationState.REMOTE_REINDEX_RUNNING, migrationActions::startRemoteReindex);


        // the state machine will stay in this state till the migration is finished or fails. It should provide
        // current migration status every time we trigger MigrationStep.REQUEST_MIGRATION_STATUS.
        config.configure(MigrationState.REMOTE_REINDEX_RUNNING)
                .permitReentry(MigrationStep.REQUEST_MIGRATION_STATUS, migrationActions::requestMigrationStatus)
                .permit(MigrationStep.RETRY_MIGRATE_EXISTING_DATA, MigrationState.MIGRATE_EXISTING_DATA) // allow one step back in case the migration fails
                .permitIf(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER, MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER, migrationActions::isRemoteReindexingFinished);

        config.configure(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .permitIf(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED, MigrationState.FINISHED, migrationActions::isOldClusterStopped);

        // in place / rolling upgrade branch of the migration
        config.configure(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .onEntry(migrationActions::rollingUpgradeSelected)
                .permit(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK, MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE, migrationActions::runDirectoryCompatibilityCheck);

        config.configure(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .permitReentryIf(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK, () -> !migrationActions.directoryCompatibilityCheckOk(), migrationActions::runDirectoryCompatibilityCheck)
                .permitIf(MigrationStep.PROVISION_DATANODE_CERTIFICATES, MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING, () -> migrationActions.directoryCompatibilityCheckOk() && !migrationActions.provisioningFinished(), migrationActions::provisionDataNodes)
                .permitIf(MigrationStep.CALCULATE_JOURNAL_SIZE, MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING, () -> migrationActions.directoryCompatibilityCheckOk() && migrationActions.provisioningFinished());

        config.configure(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .permitIf(MigrationStep.CALCULATE_JOURNAL_SIZE, MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING, migrationActions::provisioningFinished);

        config.configure(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .onEntry(migrationActions::calculateTrafficEstimate)
                .permit(MigrationStep.SHOW_STOP_PROCESSING_PAGE, MigrationState.MESSAGE_PROCESSING_STOP, migrationActions::stopMessageProcessing);

        config.configure(MigrationState.MESSAGE_PROCESSING_STOP)
                .permit(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER, MigrationState.RESTART_GRAYLOG, migrationActions::startDataNodes);

        // shows the "remove connection string, restart graylog"
        config.configure(MigrationState.RESTART_GRAYLOG)
                .permitIf(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED, MigrationState.FINISHED, migrationActions::dataNodeStartupFinished);

        return config;
    }

    @VisibleForTesting
    static StateMachine<MigrationState, MigrationStep> buildWithTestState(MigrationState state, MigrationActions migrationActions) {
        final StateMachineConfig<MigrationState, MigrationStep> config = configureStates(migrationActions);
        return new StateMachine<>(state, config);
    }


    public static StateMachine<MigrationState, MigrationStep> buildFromPersistedState(DatanodeMigrationPersistence persistence, MigrationActions migrationActions) {
        final StateMachineConfig<MigrationState, MigrationStep> statesConfiguration = configureStates(migrationActions);

        // state accessor and mutator
        final Func<MigrationState> readStateFunction = () -> persistence.getConfiguration().map(DatanodeMigrationConfiguration::currentState).orElse(MigrationState.NEW);
        final Action1<MigrationState> writeStateFunction = (currentState) -> persistence.saveConfiguration(new DatanodeMigrationConfiguration(currentState));

        return new StateMachine<>(
                readStateFunction.call(), // initial state obtained from the DB
                readStateFunction,
                writeStateFunction,
                statesConfiguration);
    }
}
