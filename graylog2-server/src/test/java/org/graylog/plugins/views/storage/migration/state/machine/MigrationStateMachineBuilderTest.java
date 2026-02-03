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

import org.junit.jupiter.api.Test;

public class MigrationStateMachineBuilderTest {

    @Test
    public void testNewState() {
        TestableMigrationActions.initialConfiguration()
                .bindToStateMachine(MigrationState.NEW)
                .assertTransition(MigrationStep.SELECT_MIGRATION);
    }

    @Test
    public void testWelcomePage() {
        TestableMigrationActions.initialConfiguration()
                .withhoutCa()
                .withoutRenewalPolicyConfigured()
                .withIncompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertState(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfiguration()
                .withhoutCa()
                .withoutRenewalPolicyConfigured()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_CA_CREATION);

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withoutRenewalPolicyConfigured()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withRenewalPolicyConfigured()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
    }

    @Test
    public void testCaCreationPage() {
        TestableMigrationActions.initialConfiguration()
                .withhoutCa()
                .withoutRenewalPolicyConfigured()
                .withDataDirCompatible()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.SHOW_CA_CREATION)
                .assertState(MigrationState.CA_CREATION_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withoutRenewalPolicyConfigured()
                .withDataDirCompatible()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withRenewalPolicyConfigured()
                .withDataDirCompatible()
                .withCompatibleIndexerVersion()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
    }

    @Test
    public void testRenewalPolicyCreationPage() {

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withoutRenewalPolicyConfigured()
                .bindToStateMachine(MigrationState.CA_CREATION_PAGE)
                .fire(MigrationStep.SHOW_RENEWAL_POLICY_CREATION)
                .assertState(MigrationState.RENEWAL_POLICY_CREATION_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withRenewalPolicyConfigured()
                .bindToStateMachine(MigrationState.CA_CREATION_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .assertState(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
    }

    @Test
    public void testRollingUpgradeMigrationWelcomePage() {
        TestableMigrationActions.initialConfiguration()
                .withCaAvailable()
                .withRenewalPolicyConfigured()
                .withDataDirCompatible()
                .withCompatibleIndexerVersion()
                .withSomeCompatibleDatanodesRunning()
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .assertState(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK);
    }

    @Test
    public void testDirectoryCompatibilityCheckPage() {
        // as long as the data dir compatibility check doesn't pass, we need to stay in the dir compatibility check state
        TestableMigrationActions.initialConfiguration()
                .withDataDirIncompatible() // <-- here is the blocking condition that prevents leaving the state
                .withSomeCompatibleDatanodesRunning()
                .bindToStateMachine(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK)
                .assertActionTriggered(TestableAction.runDirectoryCompatibilityCheck)
                .assertState(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE) // still the same state, we haven't moved
                .assertTransition(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK); // no transition to provisioning or journal

        // data dir is compatible, we can continue to certificates provisioning
        TestableMigrationActions.initialConfiguration()
                .withDataDirCompatible() // <-- compatible data dir, we can leave the check page
                .withSomeCompatibleDatanodesRunning()
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .assertTransition(MigrationStep.PROVISION_DATANODE_CERTIFICATES)
                .fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES)
                .assertActionTriggered(TestableAction.provisionDataNodes)
                .assertState(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .assertEmptyTransitions(); // as long as the provisioning is running (=not finished), there is no possible transition from here

        // datanodes are provisioned, the only possible path is to journal size and downtime warning
        TestableMigrationActions.initialConfiguration()
                .withDataDirCompatible()
                .withSomeCompatibleDatanodesRunning()
                .withDatanodesProvisioned() // <-- all datanodes are running and provisioned, we can jump to journal warning
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .assertTransition(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .fire(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .assertState(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
    }

    @Test
    public void testJournalSizeDowntimeWarning() {
        // we have compatible dir and already provisioned datanodes, we can jump straight to journal warning
        TestableMigrationActions.initialConfiguration()
                .withDataDirCompatible()
                .withSomeCompatibleDatanodesRunning()
                .withDatanodesProvisioned()
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .fire(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .assertActionTriggered(TestableAction.calculateTrafficEstimate) // <-- this is an on-entry action, which adds journal params to the context
                .assertActionTriggered(TestableAction.stopDatanodes) // <-- this is also an on-entry action
                .assertState(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
    }

    @Test
    public void testMessageProcessingStop() {
        TestableMigrationActions.initialConfiguration()
                .bindToStateMachine(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE)
                .assertActionTriggered(TestableAction.stopMessageProcessing) // <-- this is an transition action
                .assertState(MigrationState.MESSAGE_PROCESSING_STOP);

        TestableMigrationActions.initialConfiguration()
                .withOldClusterStillRunning()
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .assertEmptyTransitions(); // as long as the old cluster is running, we can't leave this state

        TestableMigrationActions.initialConfiguration()
                .withOldClusterStopped()
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertActionTriggered(TestableAction.startDataNodes) // <-- this is an transition action starting previously inactive datanodes
                .assertState(MigrationState.RESTART_GRAYLOG);
    }

    @Test
    public void testDataNodeClusterStart() {
        TestableMigrationActions.initialConfiguration()
                .withOldClusterStopped()
                .withSomeDatanodesUnavailable()
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertState(MigrationState.RESTART_GRAYLOG)
                .assertActionTriggered(TestableAction.startDataNodes)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfiguration()
                .withOldClusterStopped() // <-- if old cluster is stopped, doesn't block the data dirs
                .withAllDatanodesAvailable() // <-- and all datanodes are now running fine
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertState(MigrationState.RESTART_GRAYLOG)
                .assertActionTriggered(TestableAction.startDataNodes)
                .assertActionTriggered(TestableAction.setPreflightFinished)
                .assertTransition(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED);
    }

    @Test
    public void testFinishRollingMigration() {
        TestableMigrationActions.initialConfiguration()
                .withSomeDatanodesUnavailable() // <-- this condition prevents the finish of the migration, we have to wait for datanodes
                .bindToStateMachine(MigrationState.RESTART_GRAYLOG)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfiguration()
                .withAllDatanodesAvailable() // all datanodes available, we are done!
                .bindToStateMachine(MigrationState.RESTART_GRAYLOG)
                .fire(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED)
                .assertState(MigrationState.FINISHED);
    }
}
