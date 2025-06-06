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
import jakarta.annotation.Nonnull;
import org.graylog.plugins.views.storage.migration.state.actions.MigrationActions;
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
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_WELCOME_PAGE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MIGRATION_WELCOME_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        when(migrationActions.caDoesNotExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_CA_CREATION);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(false);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_MIGRATION_SELECTION);
    }

    @Test
    public void testCaCreationPage() {
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_WELCOME_PAGE);
        when(migrationActions.caDoesNotExist()).thenReturn(true);
        stateMachine.fire(MigrationStep.SHOW_CA_CREATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.CA_CREATION_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(2)).caDoesNotExist();
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        reset(migrationActions);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).renewalPolicyDoesNotExist();
        verify(migrationActions, times(3)).caAndRenewalPolicyExist();
        reset(migrationActions);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(false);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        verify(migrationActions, times(2)).isCompatibleInPlaceMigrationVersion();
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).renewalPolicyDoesNotExist();
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        reset(migrationActions);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.caDoesNotExist()).thenReturn(false);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(false);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions, times(2)).isCompatibleInPlaceMigrationVersion();
        verify(migrationActions, times(1)).caDoesNotExist();
        verify(migrationActions, times(1)).renewalPolicyDoesNotExist();
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRenewalPolicyCreationPage() {
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.renewalPolicyDoesNotExist()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.CA_CREATION_PAGE);
        stateMachine.fire(MigrationStep.SHOW_RENEWAL_POLICY_CREATION);
        verify(migrationActions).renewalPolicyDoesNotExist();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.RENEWAL_POLICY_CREATION_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        reset(migrationActions);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        verify(migrationActions, times(2)).isCompatibleInPlaceMigrationVersion();
        reset(migrationActions);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions, times(2)).caAndRenewalPolicyExist();
        verify(migrationActions, times(2)).isCompatibleInPlaceMigrationVersion();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testMigrationSelectionPage() {
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        when(migrationActions.caAndRenewalPolicyExist()).thenReturn(true);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.CA_CREATION_PAGE);
        stateMachine.fire(MigrationStep.SHOW_MIGRATION_SELECTION);
        verify(migrationActions).caAndRenewalPolicyExist();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MIGRATION_SELECTION_PAGE);
        reset(migrationActions);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        reset(migrationActions);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers())
                .containsOnly(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION, MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        verify(migrationActions, times(1)).isCompatibleInPlaceMigrationVersion();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRemoteReindexWelcomePage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_SELECTION_PAGE);
        when(migrationActions.compatibleDatanodesRunning()).thenReturn(true);
        when(migrationActions.isRemoteReindexMigrationEnabled()).thenReturn(true);
        stateMachine.fire(MigrationStep.SELECT_REMOTE_REINDEX_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.REMOTE_REINDEX_WELCOME_PAGE);
        verify(migrationActions).reindexUpgradeSelected();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verify(migrationActions, times(2)).allDatanodesAvailable();
        reset(migrationActions);
        when(migrationActions.allDatanodesAvailable()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_DATA_MIGRATION_QUESTION);
        verify(migrationActions, times(2)).allDatanodesAvailable();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testProvisionDatanodeCertificatesRunning() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.REMOTE_REINDEX_WELCOME_PAGE);
        when(migrationActions.compatibleDatanodesRunning()).thenReturn(true);
        stateMachine.fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verify(migrationActions, times(1)).allDatanodesAvailable();
        verify(migrationActions, times(1)).provisionAndStartDataNodes();
        reset(migrationActions);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.PROVISION_DATANODE_CERTIFICATES_RUNNING);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        verify(migrationActions, times(1)).allDatanodesAvailable();
        reset(migrationActions);
        when(migrationActions.allDatanodesAvailable()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_DATA_MIGRATION_QUESTION);
        verify(migrationActions, times(1)).allDatanodesAvailable();
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
        assertThat(stateMachine.getPermittedTriggers()).contains(MigrationStep.START_REMOTE_REINDEX_MIGRATION);
        verify(migrationActions, times(1)).getElasticsearchHosts();
        stateMachine.fire(MigrationStep.START_REMOTE_REINDEX_MIGRATION);
        verify(migrationActions, times(1)).startRemoteReindex();
        reset(migrationActions);
        when(migrationActions.isRemoteReindexingFinished()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).contains(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER, MigrationStep.REQUEST_MIGRATION_STATUS, MigrationStep.RETRY_MIGRATE_EXISTING_DATA);
        verify(migrationActions, times(1)).isRemoteReindexingFinished();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testRollingUpgradeMigrationWelcomePage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MIGRATION_SELECTION_PAGE);
        when(migrationActions.compatibleDatanodesRunning()).thenReturn(true);
        when(migrationActions.isCompatibleInPlaceMigrationVersion()).thenReturn(true);
        stateMachine.fire(MigrationStep.SELECT_ROLLING_UPGRADE_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
        verify(migrationActions).rollingUpgradeSelected();
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testDirectoryCompatibilityCheckPage() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE);
        when(migrationActions.compatibleDatanodesRunning()).thenReturn(true);
        stateMachine.fire(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.RUN_DIRECTORY_COMPATIBILITY_CHECK);
        verify(migrationActions, times(3)).directoryCompatibilityCheckOk();
        reset(migrationActions);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verify(migrationActions, times(3)).directoryCompatibilityCheckOk();
        verify(migrationActions, times(2)).provisioningFinished();
        verifyNoMoreInteractions(migrationActions);
        reset(migrationActions);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        when(migrationActions.provisioningFinished()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CALCULATE_JOURNAL_SIZE);
        verify(migrationActions, times(3)).directoryCompatibilityCheckOk();
        verify(migrationActions, times(2)).provisioningFinished();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testJournalSizeDowntimeWarning() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.DIRECTORY_COMPATIBILITY_CHECK_PAGE);
        when(migrationActions.directoryCompatibilityCheckOk()).thenReturn(true);
        stateMachine.fire(MigrationStep.PROVISION_DATANODE_CERTIFICATES);
        verify(migrationActions, times(1)).provisioningFinished();
        Mockito.when(migrationActions.allDatanodesPrepared()).thenReturn(true);
        stateMachine.fire(MigrationStep.CALCULATE_JOURNAL_SIZE);
        verify(migrationActions, times(1)).calculateTrafficEstimate();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
        verify(migrationActions, times(1)).provisionDataNodes();
        verify(migrationActions, times(1)).stopDatanodes();
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testMessageProcessingStop() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.JOURNAL_SIZE_DOWNTIME_WARNING);
        stateMachine.fire(MigrationStep.SHOW_STOP_PROCESSING_PAGE);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.MESSAGE_PROCESSING_STOP);
        verify(migrationActions).stopMessageProcessing();
        when(migrationActions.isOldClusterStopped()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        verifyNoMoreInteractions(migrationActions);
        stateMachine.fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
    }

    @Test
    public void testDataNodeClusterStart() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.MESSAGE_PROCESSING_STOP);
        when(migrationActions.isOldClusterStopped()).thenReturn(true);
        stateMachine.fire(MigrationStep.SHOW_ROLLING_UPGRADE_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.RESTART_GRAYLOG);
        assertThat(stateMachine.getPermittedTriggers()).isEmpty();
        when(migrationActions.allDatanodesAvailable()).thenReturn(true);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED);
        stateMachine.fire(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED);
    }

    @Test
    public void testFinishRollingMigration() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.RESTART_GRAYLOG);
        when(migrationActions.allDatanodesAvailable()).thenReturn(true);
        stateMachine.fire(MigrationStep.CONFIRM_OLD_CONNECTION_STRING_FROM_CONFIG_REMOVED_AND_GRAYLOG_RESTARTED);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.FINISHED);
    }

    @Test
    public void testAskToShutdownOldCluster() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.EXISTING_DATA_MIGRATION_QUESTION_PAGE);
        stateMachine.fire(MigrationStep.SKIP_EXISTING_DATA_MIGRATION);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getPermittedTriggers()).containsOnly(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED);
        verifyNoMoreInteractions(migrationActions);
    }

    @Test
    public void testAskToShutdownOldClusterFromReindexing() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.REMOTE_REINDEX_RUNNING);
        when(migrationActions.isRemoteReindexingFinished()).thenReturn(true);
        stateMachine.fire(MigrationStep.SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER);
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
    }

    @Test
    public void testManuallyRemoveOldConnectionStringFromConfig() {
        StateMachine<MigrationState, MigrationStep> stateMachine = getStateMachine(MigrationState.ASK_TO_SHUTDOWN_OLD_CLUSTER);
        stateMachine.fire(MigrationStep.CONFIRM_OLD_CLUSTER_STOPPED);
        verify(migrationActions, times(1)).finishRemoteReindexMigration();
        assertThat(stateMachine.getState()).isEqualTo(MigrationState.FINISHED);
    }

    @Nonnull
    private StateMachine<MigrationState, MigrationStep> getStateMachine(MigrationState initialState) {
        return MigrationStateMachineBuilder.buildWithTestState(initialState, migrationActions);
    }

}
