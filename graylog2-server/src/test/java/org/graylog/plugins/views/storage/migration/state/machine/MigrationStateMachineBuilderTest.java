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
        TestableMigrationActions.initialConfig()
                .bindToStateMachine(MigrationState.NEW)
                .assertTransition(MigrationStep.SELECT_MIGRATION);
    }

    @Test
    public void testWelcomePage() {
        TestableMigrationActions.initialConfig()
                .caAvailable(false)
                .renewalPolicyConfigured(false)
                .inplaceMigrationVersionCompatible(false) // <-- without compatible version, there is no migration type available
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertState(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .inplaceMigrationVersionCompatible(true)
                .caAvailable(false) // compatible version, doesn't have CA, this will be next step
                .renewalPolicyConfigured(false)
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_CA_CREATION);

        TestableMigrationActions.initialConfig()
                .inplaceMigrationVersionCompatible(true)
                .caAvailable(true)
                .renewalPolicyConfigured(false) // compatible and CA ready, needs renewal policy
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);

        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(true)
                .inplaceMigrationVersionCompatible(true) // compatible, ca + policy ready, let's continue to rolling upgrade
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
    }

    @Test
    public void testCaCreationPage() {
        TestableMigrationActions.initialConfig()
                .caAvailable(false)
                .renewalPolicyConfigured(false)
                .dataDirCompatible(false)
                .inplaceMigrationVersionCompatible(true)
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.SHOW_CA_CREATION)
                .assertState(MigrationState.CA_CREATION_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(false)
                .dataDirCompatible(false)
                .inplaceMigrationVersionCompatible(true)
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);

        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(true)
                .dataDirCompatible(true)
                .inplaceMigrationVersionCompatible(true)
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
    }

    @Test
    public void testRenewalPolicyCreationPage() {

        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(false)
                .bindToStateMachine(MigrationState.CA_CREATION_PAGE)
                .fire(MigrationStep.SHOW_RENEWAL_POLICY_CREATION)
                .assertState(MigrationState.RENEWAL_POLICY_CREATION_PAGE)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(true)
                .bindToStateMachine(MigrationState.CA_CREATION_PAGE)
                .assertTransition(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .assertState(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
    }

    @Test
    public void testRollingUpgradeMigrationWelcomePage() {
        TestableMigrationActions.initialConfig()
                .caAvailable(true)
                .renewalPolicyConfigured(true)
                .dataDirCompatible(true)
                .inplaceMigrationVersionCompatible(true)
                .someCompatibleDatanodesRunning(true)
                .bindToStateMachine(MigrationState.MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION)
                .assertActionTriggered(TestableAction.rollingUpgradeSelected)
                .assertState(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .assertTransition(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK);
    }

    @Test
    public void testDirectoryCompatibilityCheckPage() {
        // as long as the data dir compatibility check doesn't pass, we need to stay in the dir compatibility check state
        TestableMigrationActions.initialConfig()
                .dataDirCompatible(false) // <-- here is the blocking condition that prevents leaving the state
                .someCompatibleDatanodesRunning(true)
                .bindToStateMachine(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE)
                .fire(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK)
                .assertActionTriggered(TestableAction.runDirectoryCompatibilityCheck)
                .assertState(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE) // still the same state, we haven't moved
                .assertTransition(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK); // no transition to provisioning or journal

        // data dir is compatible, we can continue to certificates provisioning
        TestableMigrationActions.initialConfig()
                .dataDirCompatible(true) // <-- compatible data dir, we can leave the check page
                .someCompatibleDatanodesRunning(true)
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .assertTransition(MigrationStep.PROVISION_DATANODE_CERTIFICATES)
                .fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES)
                .assertActionTriggered(TestableAction.provisionDataNodes)
                .assertState(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .assertEmptyTransitions(); // as long as the provisioning is running (=not finished), there is no possible transition from here

        // datanodes are provisioned, the only possible path is to journal size and downtime warning
        TestableMigrationActions.initialConfig()
                .dataDirCompatible(true)
                .someCompatibleDatanodesRunning(true)
                .datanodesProvisioned(true) // <-- all datanodes are running and provisioned, we can jump to journal warning
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .assertTransition(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .fire(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .assertState(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
    }

    @Test
    public void testJournalSizeDowntimeWarning() {

        TestableMigrationActions.initialConfig()
                .allDatanodesPreparedAndWaiting(false) // some nodes are still provisioning and are not in prepared state
                .bindToStateMachine(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .allDatanodesPreparedAndWaiting(true) // all nodes prepared and waiting to opensearch start command
                .bindToStateMachine(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_RUNNING)
                .assertTransition(MigrationStep.CALCULATE_JOURNAL_SIZE);


        // we have compatible dir and already provisioned datanodes, we can jump straight to journal warning
        TestableMigrationActions.initialConfig()
                .dataDirCompatible(true)
                .someCompatibleDatanodesRunning(true)
                .datanodesProvisioned(true)
                .bindToStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE)
                .fire(MigrationStep.CALCULATE_JOURNAL_SIZE)
                .assertActionTriggered(TestableAction.calculateTrafficEstimate) // <-- this is an on-entry action, which adds journal params to the context
                .assertActionTriggered(TestableAction.stopDatanodes) // <-- this is also an on-entry action
                .assertState(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
    }

    @Test
    public void testMessageProcessingStop() {
        TestableMigrationActions.initialConfig()
                .bindToStateMachine(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING)
                .fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE)
                .assertActionTriggered(TestableAction.stopMessageProcessing) // <-- this is an transition action
                .assertState(MigrationState.MESSAGE_PROCESSING_STOP);

        TestableMigrationActions.initialConfig()
                .oldClusterStopped(false)
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .assertEmptyTransitions(); // as long as the old cluster is running, we can't leave this state

        TestableMigrationActions.initialConfig()
                .oldClusterStopped(true)
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertActionTriggered(TestableAction.startDataNodes) // <-- this is an transition action starting previously inactive datanodes
                .assertState(MigrationState.RESTART_GRAYLOG);
    }

    @Test
    public void testDataNodeClusterStart() {
        TestableMigrationActions.initialConfig()
                .oldClusterStopped(true)
                .allDatanodesStarted(false) // some are not available
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertState(MigrationState.RESTART_GRAYLOG)
                .assertActionTriggered(TestableAction.startDataNodes)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .oldClusterStopped(true) // <-- if old cluster is stopped, doesn't block the data dirs
                .allDatanodesStarted(true) // <-- and all datanodes are now running fine
                .bindToStateMachine(MigrationState.MESSAGE_PROCESSING_STOP)
                .fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER)
                .assertState(MigrationState.RESTART_GRAYLOG)
                .assertActionTriggered(TestableAction.startDataNodes)
                .assertActionTriggered(TestableAction.setPreflightFinished)
                .assertTransition(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED);
    }

    @Test
    public void testFinishRollingMigration() {
        TestableMigrationActions.initialConfig()
                .allDatanodesStarted(false) // <-- this condition prevents the finish of the migration, we have to wait for datanodes
                .bindToStateMachine(MigrationState.RESTART_GRAYLOG)
                .assertEmptyTransitions();

        TestableMigrationActions.initialConfig()
                .allDatanodesStarted(true) // all datanodes available, we are done!
                .bindToStateMachine(MigrationState.RESTART_GRAYLOG)
                .fire(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED)
                .assertState(MigrationState.FINISHED);
    }
}
