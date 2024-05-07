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
import { useState } from 'react';
import styled from 'styled-components';

import { ProgressBar } from 'components/common';
import { Alert, BootstrapModalWrapper, Button, Modal } from 'components/bootstrap';

import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';
import useRemoteReindexMigrationStatus from '../../hooks/useRemoteReindexMigrationStatus';

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

const RemoteReindexRunning = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const { nextSteps, migrationStatus, handleTriggerStep } = useRemoteReindexMigrationStatus(currentStep, onTriggerStep);
  const indicesWithErrors = migrationStatus?.indices.filter((index) => index.status === 'ERROR') || [];
  const [showLogView, setShowLogView] = useState<boolean>(false);

  return (
    <>
      We are currently migrating your existing data asynchronically,
      once the data migration is finished you will be automatically transitioned to the next step.
      <br />
      <br />
      <ProgressBar bars={[{
        animated: true,
        striped: true,
        value: migrationStatus?.progress || 0,
        bsStyle: 'info',
        label: `${migrationStatus?.status || ''} ${migrationStatus?.progress || 0}%`,
      }]} />
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
      <MigrationStepTriggerButtonToolbar nextSteps={nextSteps || currentStep.next_steps} onTriggerStep={handleTriggerStep}>
        <Button bsStyle="default" bsSize="small" onClick={() => setShowLogView(true)}>Log View</Button>
      </MigrationStepTriggerButtonToolbar>
      {showLogView && (
        <BootstrapModalWrapper showModal={showLogView}
                               onHide={() => setShowLogView(false)}
                               bsSize="large">
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
                        <tr title={new Date(log.timestamp).toLocaleString()}>
                          <td>[{log.log_level}]</td>
                          <td>{log.message}</td>
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
