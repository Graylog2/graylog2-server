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
import * as React from 'react';
import { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import type { ColorVariant } from '@graylog/sawmill';
import { useQueryParam, StringParam } from 'use-query-params';

import { ConfirmDialog } from 'components/common';
import { Alert, BootstrapModalWrapper, Button, Modal } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';
import useRemoteReindexMigrationStatus from '../../hooks/useRemoteReindexMigrationStatus';
import { MIGRATION_ACTIONS } from '../../Constants';
import RemoteReindexTasksProgress from '../common/RemoteReindexProgressBar';

const IndicesContainer = styled.div`
  max-height: 100px;
  overflow-y: auto;
`;

const LogsContainer = styled.div`
  word-break: break-all;
  overflow-wrap: break-word;
  white-space: pre-wrap;
  max-height: 500px;

  & td {
    min-width: 64px;
    vertical-align: text-top;
    padding-bottom: 4px;
  }
`;

const StyledLog = styled.span<{ $colorVariant: ColorVariant }>(({ $colorVariant, theme }) => css`
  color: ${$colorVariant ? theme.colors.variant[$colorVariant] : 'inherit'};
`);

const getColorVariantFromLogLevel = (logLovel: string): ColorVariant|undefined => {
  switch (logLovel) {
    case 'ERROR':
      return 'danger';
    case 'WARNING':
      return 'warning';
    default:
      return undefined;
  }
};

const RetryMigrateExistingData = 'RETRY_MIGRATE_EXISTING_DATA';

const RemoteReindexRunning = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { nextSteps, migrationStatus, handleTriggerStep } = useRemoteReindexMigrationStatus(currentStep, onTriggerStep);
  const indicesWithErrors = migrationStatus?.indices.filter((index) => index.status === 'ERROR') || [];
  const [showLogView, setShowLogView] = useState<boolean>(false);
  const [showRetryMigrationConfirmDialog, setShowRetryMigrationConfirmDialog] = useState<boolean>(false);
  const [showLogsQuery, setShowLogsQuery] = useQueryParam('show_logs', StringParam);
  const sendTelemetry = useSendTelemetry();

  const hasMigrationFailed = migrationStatus?.progress === 100 && migrationStatus?.status === 'ERROR';

  useEffect(() => {
    if (showLogsQuery === 'true' && !showLogView) {
      setShowLogView(true);
    }
  }, [showLogsQuery, showLogView]);

  const handleCloseLogView = () => {
    setShowLogView(false);
    setShowLogsQuery(undefined);
  };

  const handleLogViewClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_RUNNING_LOGVIEW_CLICKED, {
      app_pathname: 'datanode',
      app_section: 'migration',
    });

    setShowLogView(true);
  };

  const handleRetryClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_RUNNING_RETRY_CLICKED, {
      app_pathname: 'datanode',
      app_section: 'migration',
    });

    if (hasMigrationFailed) {
      handleTriggerStep(RetryMigrateExistingData);
    } else {
      setShowRetryMigrationConfirmDialog(true);
    }
  };

  const handleRetryConfirmClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.DATANODE_MIGRATION.REMOTEREINDEX_RUNNING_RETRY_CONFIRM_CLICKED, {
      app_pathname: 'datanode',
      app_section: 'migration',
    });

    handleTriggerStep(RetryMigrateExistingData);
    setShowRetryMigrationConfirmDialog(false);
  };

  return (
    <>
      We are currently migrating your existing data asynchronically (Graylog can be used while the reindexing is running),
      once the data migration is finished you will be automatically transitioned to the next step.
      <br />
      <br />
      <RemoteReindexTasksProgress migrationStatus={migrationStatus} />
      {(indicesWithErrors.length > 0) && (
        <Alert title="Migration failed" bsStyle="danger">
          <IndicesContainer>
            {indicesWithErrors.map((index) => (
              <span key={index.name}>
                <b>{index.name}</b>
                <p>{index.error_msg}</p>
              </span>
            ))}
          </IndicesContainer>
        </Alert>
      )}
      <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={(nextSteps || currentStep.next_steps).filter((step) => step !== RetryMigrateExistingData)} onTriggerStep={handleTriggerStep}>
        <Button bsStyle="default" bsSize="small" onClick={handleLogViewClick}>Log View</Button>
        <Button bsStyle="default" bsSize="small" onClick={handleRetryClick}>{MIGRATION_ACTIONS[RetryMigrateExistingData]?.label}</Button>
      </MigrationStepTriggerButtonToolbar>
      {showRetryMigrationConfirmDialog && (
        <ConfirmDialog show={showRetryMigrationConfirmDialog}
                       title="Retry migrating existing data"
                       onCancel={() => setShowRetryMigrationConfirmDialog(false)}
                       onConfirm={handleRetryConfirmClick}>
          Are you sure you want to stop the current running remote reindexing migration and retry migrating existing data?
        </ConfirmDialog>
      )}
      {showLogView && (
        <BootstrapModalWrapper showModal={showLogView}
                               onHide={handleCloseLogView}
                               bsSize="large"
                               backdrop>
          <Modal.Header closeButton>
            <Modal.Title>Remote Reindex Migration Logs</Modal.Title>
          </Modal.Header>
          <Modal.Body>
            <pre>
              {migrationStatus?.logs ? (
                <LogsContainer>
                  <table>
                    <tbody>
                      {migrationStatus.logs.map((log) => (
                        <tr>
                          <td width={180}>{new Date(log.timestamp).toLocaleString()}</td>
                          <td width={80}>[<StyledLog $colorVariant={getColorVariantFromLogLevel(log.log_level)}>{log.log_level}</StyledLog>]</td>
                          <td><StyledLog $colorVariant={getColorVariantFromLogLevel(log.log_level)}>{log.message}</StyledLog></td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </LogsContainer>
              ) : ('No logs.')}
            </pre>
          </Modal.Body>
        </BootstrapModalWrapper>
      )}
    </>
  );
};

export default RemoteReindexRunning;
