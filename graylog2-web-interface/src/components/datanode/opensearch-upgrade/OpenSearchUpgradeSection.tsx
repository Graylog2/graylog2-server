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
import { useTranslation } from 'react-i18next';

import { Alert, Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { Title } from 'components/common';
import useIncompatibleIndices from 'components/indices/hooks/useIncompatibleIndices';
import IncompatibleIndicesTable from 'components/indices/incompatible-indices/IncompatibleIndicesTable';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import ForceStartConfirmDialog from './ForceStartConfirmDialog';
import StartUpgradeConfirmDialog from './StartUpgradeConfirmDialog';
import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import useOpenSearchRollingRestart, { rollingRestartStartError } from './hooks/useOpenSearchRollingRestart';
import useOpenSearchUpgradeStatus from './hooks/useOpenSearchUpgradeStatus';
import OpenSearchUpgradeInfo from './OpenSearchUpgradeInfo';
import OpenSearchRollingUpgradeNodes from './OpenSearchRollingUpgradeNodes';
import { isRollingRestartPaused, isRollingRestartTerminalState } from './rollingRestartTypes';

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

const ActionsRow = styled(Row)(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
  `,
);

const Heading = styled.h4(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const HelpText = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.md};
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

const TableContainer = styled.div(
  ({ theme }) => css`
    margin-top: ${theme.spacings.lg};
  `,
);

const HelpTextHeading = styled.h4`
  margin: 0;
  color: inherit;
  font-size: inherit;
`;

const HelpTextList = styled.ul(
  ({ theme }) => css`
    margin: ${theme.spacings.xs} 0 0;
    padding-left: ${theme.spacings.lg};
    list-style-position: outside;
    list-style-type: disc;

    > li {
      display: list-item;
      margin-bottom: ${theme.spacings.xxs};
    }

    > li:last-child {
      margin-bottom: 0;
    }
  `,
);

const MIN_NODES_FOR_ROLLING_UPGRADE = 3;
const TELEMETRY_DEFAULTS = { app_pathname: 'datanode' } as const;

const IncompatibleIndicesInfo = () => {
  const { t } = useTranslation('datanode');

  return (
    <HelpText>
      <HelpTextHeading>{t('opensearch_upgrade.before_upgrading')}</HelpTextHeading>
      <HelpTextList>
        <li>{t('opensearch_upgrade.old_indices_unreadable')}</li>
        <li>{t('opensearch_upgrade.use_table')}</li>
      </HelpTextList>
    </HelpText>
  );
};

const UpgradeProcessInfo = ({ isRollingUpgrade }: { isRollingUpgrade: boolean }) => {
  const { t } = useTranslation('datanode');

  return (
    <Row>
      <Col xs={12}>
        <HelpText>
          <HelpTextHeading>{t('opensearch_upgrade.upgrade_process')}</HelpTextHeading>
          <HelpTextList>
            <li>{t('opensearch_upgrade.automatic_upgrade')}</li>
            {isRollingUpgrade ? (
              <>
                <li>{t('opensearch_upgrade.rolling_restart')}</li>
                <li>{t('opensearch_upgrade.rolling_search_impaired')}</li>
              </>
            ) : (
              <>
                <li>{t('opensearch_upgrade.full_restart')}</li>
                <li>{t('opensearch_upgrade.full_search_unavailable')}</li>
              </>
            )}
            <li>{t('opensearch_upgrade.backup')}</li>
          </HelpTextList>
        </HelpText>
      </Col>
    </Row>
  );
};

const OpenSearchUpgradeSection = () => {
  const { t } = useTranslation('datanode');
  const {
    currentVersion,
    targetVersion,
    nodes: openSearchVersionNodes,
    numberOfDataNodes,
    unavailableDataNodeCount,
    isLoading,
    refetch: refetchOpenSearchClusterStats,
  } = useOpenSearchClusterStats();
  const openSearchStatus = useOpenSearchUpgradeStatus();
  const {
    data: incompatibleIndices,
    isError: isIncompatibleIndicesError,
    isLoading: isLoadingIncompatibleIndices,
  } = useIncompatibleIndices();
  const {
    data: rollingRestart,
    isResumingRollingRestart,
    isStartingRollingRestart,
    resumeRollingRestart,
    startRollingRestart,
  } = useOpenSearchRollingRestart();
  const sendTelemetry = useSendTelemetry('opensearch-upgrade');
  const [forceStartFailedChecks, setForceStartFailedChecks] = useState<Array<string>>([]);
  const [showStartConfirm, setShowStartConfirm] = useState(false);
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasIncompatibleIndices = incompatibleIndices.length > 0;
  const areIncompatibleIndicesResolved =
    !isLoadingIncompatibleIndices && !isIncompatibleIndicesError && !hasIncompatibleIndices;
  const rollingRestartState = rollingRestart?.data?.sm_state;
  const hasRollingRestartJob = !!rollingRestart?.data;
  const showIncompatibleIndices = !hasRollingRestartJob && !areIncompatibleIndicesResolved;
  const showStartAction = openSearchStatus === 'outdated';
  const showUpgradeProcess = showStartAction && areIncompatibleIndicesResolved;
  const isStartActionDisabled = isStartingRollingRestart || !areIncompatibleIndicesResolved;
  const startActionLabel = isRollingUpgradePossible
    ? t('opensearch_upgrade.start_rolling_upgrade')
    : t('opensearch_upgrade.start_upgrade_and_restart');
  const startActionLoadingLabel = isRollingUpgradePossible
    ? t('opensearch_upgrade.starting_rolling_upgrade')
    : t('opensearch_upgrade.restarting');
  const canResumeRollingRestart =
    isRollingRestartPaused(rollingRestart?.data?.sm_state) && !rollingRestart?.data?.abort_requested;
  const showRollingUpgradeStatus = hasRollingRestartJob;

  useEffect(() => {
    if (isRollingRestartTerminalState(rollingRestartState)) {
      refetchOpenSearchClusterStats();
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
    handleStartRollingRestart(true);
  };

  const handleResumeClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_OPENSEARCH_UPGRADE.ROLLING_UPGRADE_RESUMED, TELEMETRY_DEFAULTS);
    resumeRollingRestart();
  };

  if (openSearchStatus === 'unconfirmed') {
    return (
      <Section>
        <Title order={1}>{t('opensearch_upgrade.title')}</Title>
        <OpenSearchUpgradeInfo
          currentVersion={currentVersion}
          targetVersion={targetVersion}
          isLoading={isLoading}
          availableDataNodes={numberOfDataNodes}
          unavailableDataNodes={unavailableDataNodeCount}
        />
        <Alert bsStyle="warning">{t('opensearch_upgrade.unconfirmed', { count: unavailableDataNodeCount })}</Alert>
      </Section>
    );
  }

  return (
    <Section>
      <Title order={1}>{t('opensearch_upgrade.title')}</Title>
      <OpenSearchUpgradeInfo
        currentVersion={currentVersion}
        targetVersion={targetVersion}
        isLoading={isLoading}
        availableDataNodes={numberOfDataNodes}
        unavailableDataNodes={unavailableDataNodeCount}
      />

      {showIncompatibleIndices && (
        <Row>
          <Col xs={12}>
            <Heading>{t('opensearch_upgrade.incompatible_indices')}</Heading>
            {hasIncompatibleIndices && <IncompatibleIndicesInfo />}
            <TableContainer>
              <IncompatibleIndicesTable />
            </TableContainer>
          </Col>
        </Row>
      )}

      {showUpgradeProcess && <UpgradeProcessInfo isRollingUpgrade={isRollingUpgradePossible} />}

      <ActionsRow>
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
                {isResumingRollingRestart ? t('opensearch_upgrade.resuming') : t('opensearch_upgrade.resume')}
              </Button>
            )}
          </ButtonToolbar>
          {showIncompatibleIndices && hasIncompatibleIndices && (
            <DisabledHint>{t('opensearch_upgrade.resolve_indices_first')}</DisabledHint>
          )}
          {openSearchStatus === 'error' && <DisabledHint>{t('opensearch_upgrade.check_failed')}</DisabledHint>}
          {openSearchStatus === 'up-to-date' && (
            <DisabledHint>{t('opensearch_upgrade.already_up_to_date')}</DisabledHint>
          )}
        </Col>
      </ActionsRow>

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
