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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { Alert, Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import ForceStartConfirmDialog from './ForceStartConfirmDialog';
import StartUpgradeConfirmDialog from './StartUpgradeConfirmDialog';
import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import useOpenSearchRollingRestart, { rollingRestartStartError } from './hooks/useOpenSearchRollingRestart';
import OutdatedIndicesTable from './OutdatedIndicesTable';
import OpenSearchUpgradeInfo from './OpenSearchUpgradeInfo';
import OpenSearchRollingUpgradeNodes from './OpenSearchRollingUpgradeNodes';
import { isRollingRestartActive, isRollingRestartTerminalState } from './rollingRestartTypes';

const Section = styled.div(
  ({ theme }) => css`
    border-top: 1px solid ${theme.colors.variant.default};
    padding-top: ${theme.spacings.lg};
  `,
);

const DisabledHint = styled.p(
  ({ theme }) => css`
    margin-top: ${theme.spacings.xs};
    margin-bottom: 0;
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

const MIN_NODES_FOR_ROLLING_UPGRADE = 3;
const TELEMETRY_DEFAULTS = { app_pathname: 'datanode', app_section: 'opensearch-upgrade' } as const;

const OpenSearchUpgradeSection = () => {
  const {
    currentVersion,
    targetVersion,
    nodes: openSearchVersionNodes,
    numberOfDataNodes,
    unavailableDataNodeCount,
    isError: isVersionOverviewError,
    isLoading,
    isUpgradeAvailable,
    refetch: refetchOpenSearchClusterStats,
  } = useOpenSearchClusterStats();
  const {
    data: outdatedIndices,
    isError: isOutdatedIndicesError,
    isLoading: isLoadingOutdatedIndices,
  } = useOutdatedIndices();
  const {
    data: rollingRestart,
    isResumingRollingRestart,
    isStartingRollingRestart,
    resumeRollingRestart,
    startRollingRestart,
  } = useOpenSearchRollingRestart();
  const sendTelemetry = useSendTelemetry();
  const [forceStartFailedChecks, setForceStartFailedChecks] = useState<Array<string>>([]);
  const [showStartConfirm, setShowStartConfirm] = useState(false);
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasOutdatedIndices = outdatedIndices.length > 0;
  const rollingRestartState = rollingRestart?.data?.sm_state;
  const hasActiveRollingRestart = isRollingRestartActive(rollingRestart);
  // A job (running or finished) owns the section: hide the outdated-indices list, show its status panel.
  const hasRollingRestartJob = !!rollingRestart?.data;
  const showOutdatedIndices = !hasRollingRestartJob;
  const showStartAction = !hasActiveRollingRestart && isUpgradeAvailable;
  // Only the initial load counts as "checking" — background refetches (the 5s poll) must not blank the status
  // table or flip the start button, otherwise the whole section flickers on every poll.
  const isCheckingVersionOverview = isLoading;
  const isStartActionDisabled =
    isStartingRollingRestart ||
    isCheckingVersionOverview ||
    isVersionOverviewError ||
    isLoadingOutdatedIndices ||
    isOutdatedIndicesError ||
    hasOutdatedIndices;
  // Below the 3-node floor a rolling upgrade isn't possible, so the action is a plain (full) restart.
  const startActionLabel = isRollingUpgradePossible ? 'Start OpenSearch Rolling Upgrade' : 'Restart';
  const startActionLoadingLabel = isRollingUpgradePossible ? 'Starting OpenSearch Rolling Upgrade...' : 'Restarting...';
  const canResumeRollingRestart =
    rollingRestart?.data?.sm_state === 'PAUSED_WAITING_GREEN' && !rollingRestart.data.abort_requested;
  // Not gated on indices, so a finished FAILED/ABORTED run keeps showing its outcome.
  const showRollingUpgradeStatus = hasRollingRestartJob;

  useEffect(() => {
    if (isRollingRestartTerminalState(rollingRestartState)) {
      void refetchOpenSearchClusterStats();
    }
  }, [refetchOpenSearchClusterStats, rollingRestartState]);

  const handleStartRollingRestart = async (force: boolean = false) => {
    try {
      await startRollingRestart(force);
      setForceStartFailedChecks([]);
    } catch (error) {
      const startError = rollingRestartStartError(error);

      if (!force && startError.canRetryWithForce) {
        setForceStartFailedChecks(startError.failedChecks);
      }
    }
  };

  const handleStartConfirm = async () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_STARTED, {
      ...TELEMETRY_DEFAULTS,
      event_details: { number_of_data_nodes: numberOfDataNodes },
    });
    await handleStartRollingRestart();
    setShowStartConfirm(false);
  };

  const handleForceStartConfirm = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_FORCE_STARTED, TELEMETRY_DEFAULTS);
    void handleStartRollingRestart(true);
  };

  const handleResumeClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_RESUMED, TELEMETRY_DEFAULTS);
    void resumeRollingRestart();
  };

  // Versions look equal but can't be confirmed while nodes are down — hide the operational panels.
  // Active upgrades are exempt: a temporarily missing node is their normal condition.
  const isVersionStateUnconfirmed =
    !isCheckingVersionOverview && !isVersionOverviewError && !isUpgradeAvailable && unavailableDataNodeCount > 0;

  if (isVersionStateUnconfirmed && !hasActiveRollingRestart) {
    return (
      <Section>
        <h1>Upgrade Data Nodes&apos; embedded OpenSearch</h1>
        <OpenSearchUpgradeInfo
          currentVersion={currentVersion}
          targetVersion={targetVersion}
          isLoading={isLoading}
          availableDataNodes={numberOfDataNodes}
          unavailableDataNodes={unavailableDataNodeCount}
        />
        <Alert bsStyle="warning">
          Outdated indices and upgrade status cannot be checked while {unavailableDataNodeCount} Data{' '}
          {unavailableDataNodeCount === 1 ? 'Node is' : 'Nodes are'} unavailable — they will show again once all
          Data Nodes are available.
        </Alert>
      </Section>
    );
  }

  return (
    <Section>
      <h1>Upgrade Data Nodes&apos; embedded OpenSearch</h1>
      <OpenSearchUpgradeInfo
        currentVersion={currentVersion}
        targetVersion={targetVersion}
        isLoading={isLoading}
        availableDataNodes={numberOfDataNodes}
        unavailableDataNodes={unavailableDataNodeCount}
      />

      {showOutdatedIndices && (
        <Row>
          <Col xs={12}>
            <OutdatedIndicesTable />
          </Col>
        </Row>
      )}

      <Row>
        <Col xs={12}>
          <ButtonToolbar>
            {showStartAction && (
              <Button
                bsStyle="primary"
                onClick={() => setShowStartConfirm(true)}
                disabled={isStartActionDisabled}
                type="button">
                {isStartingRollingRestart ? startActionLoadingLabel : startActionLabel}
              </Button>
            )}
            {canResumeRollingRestart && (
              <Button bsStyle="primary" disabled={isResumingRollingRestart} onClick={handleResumeClick} type="button">
                {isResumingRollingRestart ? 'Resuming...' : 'Resume'}
              </Button>
            )}
          </ButtonToolbar>
          {showOutdatedIndices && hasOutdatedIndices && (
            <DisabledHint>Resolve all outdated indices first.</DisabledHint>
          )}
          {!hasActiveRollingRestart && isVersionOverviewError && (
            <DisabledHint>Could not check OpenSearch upgrade availability.</DisabledHint>
          )}
          {!hasActiveRollingRestart && !isCheckingVersionOverview && !isVersionOverviewError && !isUpgradeAvailable && (
            <DisabledHint>Data Nodes&apos; embedded OpenSearch is already up to date.</DisabledHint>
          )}
        </Col>
      </Row>

      {showRollingUpgradeStatus && (
        <Row>
          <Col xs={12}>
            <OpenSearchRollingUpgradeNodes job={rollingRestart} versionNodes={openSearchVersionNodes} />
          </Col>
        </Row>
      )}
      {showStartConfirm && (
        <StartUpgradeConfirmDialog
          currentVersion={currentVersion}
          targetVersion={targetVersion}
          numberOfDataNodes={numberOfDataNodes}
          isRollingUpgrade={isRollingUpgradePossible}
          isSubmitting={isStartingRollingRestart}
          onCancel={() => setShowStartConfirm(false)}
          onConfirm={handleStartConfirm}
        />
      )}
      {!!forceStartFailedChecks.length && (
        <ForceStartConfirmDialog
          failedChecks={forceStartFailedChecks}
          isSubmitting={isStartingRollingRestart}
          onCancel={() => setForceStartFailedChecks([])}
          onConfirm={handleForceStartConfirm}
        />
      )}
    </Section>
  );
};

export default OpenSearchUpgradeSection;
