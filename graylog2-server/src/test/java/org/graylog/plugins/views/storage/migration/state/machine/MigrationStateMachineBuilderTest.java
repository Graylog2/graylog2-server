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
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MigrationStateMachineBuilderTest {

    @Mock
    MigrationActions migrationActions;

    @Test
    public void testNewState() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.NEW);
        assertThat(stateMachine.getPermittedTriggers()).containsExactly(MigrationStep.SELECT_MIGRATION);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testWelcomePage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_WELCOME_PAGE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MIGRATION_WELCOME_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_DIRECTORY_COMPATIBILITY_CHECK, MigrationStep.SKIP_DIRECTORY_COMPATIBILITY_CHECK);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testDirectoryCompatibilityCheck() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_WELCOME_PAGE);
        stateMachine.fire(MigrationStep.SHOW_DIRECTORY_COMPATIBILITY_CHECK);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE);
        verify(migrationActions).runDirectoryCompatibilityCheck();
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).directoryCompatibilityCheckOk();
        reset(migrationActions);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_CA_CREATION);
        verify(migrationActions, times(1)).directoryCompatibilityCheckOk();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testCaCreationPage() {
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE);
        stateMachine.fire(MigrationStep.SHOW_CA_CREATION);
        verify(migrationActions).directoryCompatibilityCheckOk();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.CA_CREATION_PAGE);
        when(migrationActions.caDoesNotExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).caAndRemovalPolicyExist();
        reset(migrationActions);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).caAndRemovalPolicyExist();
        reset(migrationActions);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.caAndRemovalPolicyExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_RENEWAL_POLICY_CREATION, MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).caAndRemovalPolicyExist();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRenewalPolicyCreationPage() {
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.CA_CREATION_PAGE);
        stateMachine.fire(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);
        verify(migrationActions).caDoesNotExist();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.RENEWAL_POLICY_CREATION_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).caAndRemovalPolicyExist();
        reset(migrationActions);
        when(migrationActions.caAndRemovalPolicyExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions, times(1)).caAndRemovalPolicyExist();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testMigrationSelectionPage() {
        when(migrationActions.caAndRemovalPolicyExist()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.CA_CREATION_PAGE);
        stateMachine.fire(MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions).caAndRemovalPolicyExist();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MIGRATION_SELECTION_PAGE);
        assertThat(stateMachine.getPermittedTriggers())
                .containsOnly(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION, MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRemoteReindexWelcomePage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_SELECTION_PAGE);
        stateMachine.fire(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.REMOTE_REINDEX_WELCOME_PAGE);
        verify(migrationActions).reindexUpgradeSelected();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.DISCOVER_NEW_DATANODES);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testProvisionDatanodeCertificatesPage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.REMOTE_REINDEX_WELCOME_PAGE);
        stateMachine.fire(MigrationStep.DISCOVER_NEW_DATANODES);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.PROVISION_DATANODE_CERTIFICATES_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testProvisionDatanodeCertificatesRunning() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.PROVISION_DATANODE_CERTIFICATES_PAGE);
        stateMachine.fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verify(migrationActions, times(1)).provisionDataNodes();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).dataNodeStartupFinished();
        reset(migrationActions);
        when(migrationActions.dataNodeStartupFinished()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_DATA_MIGRATION_QUESTION);
        verify(migrationActions, times(1)).dataNodeStartupFinished();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testExistingDataMigrationQuestionPage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_MIGRATE_EXISTING_DATA, MigrationStep.SKIP_EXISTING_DATA_MIGRATION);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testMigrateExistingData() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE);
        stateMachine.fire(MigrationStep.SHOW_MIGRATE_EXISTING_DATA);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MIGRATE_EXISTING_DATA);
        verify(migrationActions).reindexOldData();
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).reindexingFinished();
        reset(migrationActions);
        when(migrationActions.reindexingFinished()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        verify(migrationActions, times(1)).reindexingFinished();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRollingUpgradeMigrationWelcomePage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_SELECTION_PAGE);
        stateMachine.fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
        verify(migrationActions).rollingUpgradeSelected();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.INSTALL_DATANODES_ON_EVERY_NODE);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testDirectoryCompatibilityCheckPage2() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
        stateMachine.fire(MigrationStep.INSTALL_DATANODES_ON_EVERY_NODE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE2);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).directoryCompatibilityCheckOk();
        reset(migrationActions);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES);
        verify(migrationActions, times(1)).directoryCompatibilityCheckOk();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testProvisionRollingUpgradeNodesWithCertificates() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE2);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        stateMachine.fire(MigrationStep.SHOW_PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES);
        verify(migrationActions, times(1)).directoryCompatibilityCheckOk();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testJournalSizeDowntimeWarning() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES);
        stateMachine.fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        Mockito.when(migrationActions.provisioningFinished()).thenReturn(true);
        stateMachine.fire(MigrationStep.CALCULATE_JOURNAL_SIZE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
        verify(migrationActions, times(1)).provisionDataNodes();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testMessageProcessingStop() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
        stateMachine.fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MESSAGE_PROCESSING_STOP);
        verify(migrationActions).stopMessageProcessing();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        verifyNoMoreInteractions(migrationActions);
        stateMachine.fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
    }

    @Test
    public void testDataNodeClusterStart() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MESSAGE_PROCESSING_STOP);
        stateMachine.fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.REPLACE_CLUSTER);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_ASK_TO_RESTART_MESSAGE_PROCESSING);
        stateMachine.fire(MigrationStep.SHOW_ASK_TO_RESTART_MESSAGE_PROCESSING);
    }

    @Test
    public void testMessageProcessingRestart() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.REPLACE_CLUSTER);
        stateMachine.fire(MigrationStep.SHOW_ASK_TO_RESTART_MESSAGE_PROCESSING);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MESSAGE_PROCESSING_RESTART);
        verify(migrationActions).startMessageProcessing();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CONFIRM_ROLLING_UPGRADE_OLD_CLUSTER_STOPPED);
        stateMachine.fire(MigrationStep.CONFIRM_ROLLING_UPGRADE_OLD_CLUSTER_STOPPED);
    }

    @Test
    public void testAskToShutdownOldCluster() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE);
        stateMachine.fire(MigrationStep.SKIP_EXISTING_DATA_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).isOldClusterStopped();
        reset(migrationActions);
        when(migrationActions.isOldClusterStopped()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED);
        verify(migrationActions, times(1)).isOldClusterStopped();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testAskToShutdownOldClusterFromReindexing() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATE_EXISTING_DATA);
        when(migrationActions.reindexingFinished()).thenReturn(true);
        stateMachine.fire(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
    }

    @Test
    public void testManuallyRemoveOldConnectionStringFromConfig() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
        when(migrationActions.isOldClusterStopped()).thenReturn(true);
        stateMachine.fire(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED);
        stateMachine.fire(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.FINISHED);
    }

    @NotNull
    private StateMachine<MigrationState, MigrationStep> getStateMachine(MigrationState initialState) {
        return MigrationStateMachineBuilder.buildWithTestState(initialState, migrationActions);
    }

}
