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

import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import ForceStartConfirmDialog from './ForceStartConfirmDialog';
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
    isError: isVersionOverviewError,
    isFetching: isFetchingVersionOverview,
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
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasOutdatedIndices = outdatedIndices.length > 0;
  const rollingRestartState = rollingRestart?.data?.sm_state;
  const hasActiveRollingRestart = isRollingRestartActive(rollingRestart);
  const showStartAction = !hasActiveRollingRestart && isUpgradeAvailable;
  const isCheckingVersionOverview = isLoading || isFetchingVersionOverview;
  const isStartActionDisabled =
    isStartingRollingRestart ||
    isCheckingVersionOverview ||
    isVersionOverviewError ||
    isLoadingOutdatedIndices ||
    isOutdatedIndicesError ||
    hasOutdatedIndices;
  const startActionLabel = isRollingUpgradePossible
    ? 'Start OpenSearch Rolling Upgrade'
    : 'Apply OpenSearch Upgrade on Next Restart';
  const startActionLoadingLabel = isRollingUpgradePossible
    ? 'Starting OpenSearch Rolling Upgrade...'
    : 'Applying OpenSearch Upgrade...';
  const canResumeRollingRestart =
    rollingRestart?.data?.sm_state === 'PAUSED_WAITING_GREEN' && !rollingRestart.data.abort_requested;
  const showRollingUpgradeStatus =
    !!rollingRestart &&
    (hasActiveRollingRestart || (!isCheckingVersionOverview && !isVersionOverviewError && !isUpgradeAvailable));

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

  const handleStartClick = () => {
    sendTelemetry(
      isRollingUpgradePossible
        ? TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_STARTED
        : TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.APPLY_ON_NEXT_RESTART_CLICKED,
      { ...TELEMETRY_DEFAULTS, event_details: { number_of_data_nodes: numberOfDataNodes } },
    );
    void handleStartRollingRestart();
  };

  const handleForceStartConfirm = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_FORCE_STARTED, TELEMETRY_DEFAULTS);
    void handleStartRollingRestart(true);
  };

  const handleResumeClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_RESUMED, TELEMETRY_DEFAULTS);
    void resumeRollingRestart();
  };

  return (
    <Section>
      <h1>Upgrade Data Nodes&apos; embedded OpenSearch</h1>
      <OpenSearchUpgradeInfo currentVersion={currentVersion} targetVersion={targetVersion} isLoading={isLoading} />

      <Row>
        <Col xs={12}>
          <OutdatedIndicesTable />
        </Col>
      </Row>

      <Row>
        <Col xs={12}>
          <ButtonToolbar>
            {showStartAction && (
              <Button bsStyle="primary" onClick={handleStartClick} disabled={isStartActionDisabled} type="button">
                {isStartingRollingRestart ? startActionLoadingLabel : startActionLabel}
              </Button>
            )}
            {canResumeRollingRestart && (
              <Button bsStyle="primary" disabled={isResumingRollingRestart} onClick={handleResumeClick} type="button">
                {isResumingRollingRestart ? 'Resuming...' : 'Resume'}
              </Button>
            )}
          </ButtonToolbar>
          {!hasActiveRollingRestart && isVersionOverviewError && (
            <DisabledHint>Could not check OpenSearch upgrade availability.</DisabledHint>
          )}
          {!hasActiveRollingRestart && isFetchingVersionOverview && !isLoading && (
            <DisabledHint>Refreshing OpenSearch upgrade status...</DisabledHint>
          )}
          {!hasActiveRollingRestart && !isCheckingVersionOverview && !isVersionOverviewError && !isUpgradeAvailable && (
            <DisabledHint>Data Nodes&apos; embedded OpenSearch is already up to date.</DisabledHint>
          )}
          {isLoadingOutdatedIndices && <DisabledHint>Checking outdated indices...</DisabledHint>}
          {isOutdatedIndicesError && (
            <DisabledHint>Reload outdated indices before starting the OpenSearch upgrade.</DisabledHint>
          )}
          {hasOutdatedIndices && <DisabledHint>Resolve all outdated indices first.</DisabledHint>}
        </Col>
      </Row>

      {showRollingUpgradeStatus && (
        <Row>
          <Col xs={12}>
            <OpenSearchRollingUpgradeNodes job={rollingRestart} versionNodes={openSearchVersionNodes} />
          </Col>
        </Row>
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
