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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { ConfirmDialog, Spinner } from 'components/common';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';

import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import useOpenSearchRollingRestart, { rollingRestartStartError } from './hooks/useOpenSearchRollingRestart';
import { outdatedIndicesMockOverride } from './mockOutdatedIndices';
import OutdatedIndicesTable from './OutdatedIndicesTable';
import OpenSearchRollingUpgradeNodes from './OpenSearchRollingUpgradeNodes';
import { isRollingRestartActive } from './rollingRestartTypes';

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

const InfoList = styled.dl(
  ({ theme }) => css`
    margin: ${theme.spacings.md} 0;

    > dt {
      clear: left;
      float: left;
      margin-bottom: ${theme.spacings.sm};
      width: 240px;
    }

    > dd {
      margin-bottom: ${theme.spacings.sm};
      margin-left: 240px;
    }
  `,
);

const MIN_NODES_FOR_ROLLING_UPGRADE = 3;

const OpenSearchUpgradeInfo = ({
  currentVersion,
  targetVersion,
  isLoading,
}: {
  currentVersion: string | undefined;
  targetVersion: string | undefined;
  isLoading: boolean;
}) => (
  <InfoList>
    <dt>Current OpenSearch version:</dt>
    <dd>{isLoading ? <Spinner text="Loading..." /> : currentVersion || 'Unknown'}</dd>
    <dt>Target OpenSearch version:</dt>
    <dd>{isLoading ? <Spinner text="Loading..." /> : targetVersion || 'Unknown'}</dd>
  </InfoList>
);

const ForceStartConfirmDialog = ({
  failedChecks,
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  failedChecks: Array<string>;
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) => (
  <ConfirmDialog
    show
    title="Start OpenSearch rolling upgrade anyway?"
    btnConfirmText="Start anyway"
    isAsyncSubmit
    isSubmitting={isSubmitting}
    onCancel={onCancel}
    onConfirm={onConfirm}
    submitLoadingText="Starting...">
    <p>The backend reported that the normal preflight checks did not pass.</p>
    <ul>
      {failedChecks.map((failedCheck) => (
        <li key={failedCheck}>{failedCheck}</li>
      ))}
    </ul>
  </ConfirmDialog>
);

const AbortConfirmDialog = ({
  isSubmitting,
  onCancel,
  onConfirm,
}: {
  isSubmitting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) => (
  <ConfirmDialog
    show
    title="Abort OpenSearch rolling upgrade?"
    btnConfirmText="Abort"
    isAsyncSubmit
    isSubmitting={isSubmitting}
    onCancel={onCancel}
    onConfirm={onConfirm}
    submitLoadingText="Aborting...">
    <p>The current step may finish before the rolling upgrade stops.</p>
  </ConfirmDialog>
);

const OpenSearchUpgradeSection = () => {
  const { currentVersion, targetVersion, numberOfDataNodes, isLoading } = useOpenSearchClusterStats();
  const { data: outdatedIndices } = useOutdatedIndices({ mockData: outdatedIndicesMockOverride });
  const {
    abortRollingRestart,
    data: rollingRestart,
    isAbortingRollingRestart,
    isResumingRollingRestart,
    isStartingRollingRestart,
    resumeRollingRestart,
    startRollingRestart,
  } = useOpenSearchRollingRestart();
  const [forceStartFailedChecks, setForceStartFailedChecks] = useState<Array<string>>([]);
  const [showAbortConfirmDialog, setShowAbortConfirmDialog] = useState(false);
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasOutdatedIndices = outdatedIndices.length > 0;
  const hasActiveRollingRestart = isRollingRestartActive(rollingRestart);
  const canResumeRollingRestart =
    rollingRestart?.data?.sm_state === 'PAUSED_WAITING_GREEN' && !rollingRestart.data.abort_requested;

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

  const handleAbortRollingRestart = async () => {
    await abortRollingRestart();
    setShowAbortConfirmDialog(false);
  };

  return (
    <Section>
      <h1>Upgrade Data Node&apos;s embedded OpenSearch</h1>
      <OpenSearchUpgradeInfo currentVersion={currentVersion} targetVersion={targetVersion} isLoading={isLoading} />

      <Row>
        <Col xs={12}>
          <OutdatedIndicesTable />
        </Col>
      </Row>

      <Row>
        <Col xs={12}>
          <ButtonToolbar>
            {isRollingUpgradePossible ? (
              <Button
                bsStyle="primary"
                onClick={() => {
                  void handleStartRollingRestart();
                }}
                disabled={hasOutdatedIndices || hasActiveRollingRestart || isStartingRollingRestart}
                type="button">
                {isStartingRollingRestart
                  ? 'Starting OpenSearch Rolling Upgrade...'
                  : 'Start OpenSearch Rolling Upgrade'}
              </Button>
            ) : (
              <Button bsStyle="default" onClick={() => {}} disabled={hasOutdatedIndices} type="button">
                Apply OpenSearch upgrade on next restart
              </Button>
            )}
            {canResumeRollingRestart && (
              <Button
                bsStyle="primary"
                disabled={isResumingRollingRestart}
                onClick={() => {
                  void resumeRollingRestart();
                }}
                type="button">
                {isResumingRollingRestart ? 'Resuming...' : 'Resume'}
              </Button>
            )}
            {hasActiveRollingRestart && (
              <Button
                bsStyle="danger"
                disabled={isAbortingRollingRestart}
                onClick={() => setShowAbortConfirmDialog(true)}
                type="button">
                {isAbortingRollingRestart ? 'Aborting...' : 'Abort'}
              </Button>
            )}
          </ButtonToolbar>
          {hasOutdatedIndices && <DisabledHint>Resolve all outdated indices first.</DisabledHint>}
          {hasActiveRollingRestart && <DisabledHint>An OpenSearch rolling upgrade is already active.</DisabledHint>}
        </Col>
      </Row>

      <Row>
        <Col xs={12}>
          <OpenSearchRollingUpgradeNodes job={rollingRestart} />
        </Col>
      </Row>
      {!!forceStartFailedChecks.length && (
        <ForceStartConfirmDialog
          failedChecks={forceStartFailedChecks}
          isSubmitting={isStartingRollingRestart}
          onCancel={() => setForceStartFailedChecks([])}
          onConfirm={() => {
            void handleStartRollingRestart(true);
          }}
        />
      )}
      {showAbortConfirmDialog && (
        <AbortConfirmDialog
          isSubmitting={isAbortingRollingRestart}
          onCancel={() => setShowAbortConfirmDialog(false)}
          onConfirm={() => {
            void handleAbortRollingRestart();
          }}
        />
      )}
    </Section>
  );
};

export default OpenSearchUpgradeSection;
