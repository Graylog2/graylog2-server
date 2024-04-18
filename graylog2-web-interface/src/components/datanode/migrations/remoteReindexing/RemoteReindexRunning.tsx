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
import styled from 'styled-components';

import { ProgressBar } from 'components/common';
import { Alert } from 'components/bootstrap';

import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';
import useRemoteReindexMigrationStatus from '../../hooks/useRemoteReindexMigrationStatus';

const IndicesContainer = styled.div`
  max-height: 100px;
  overflow-y: auto;
`;

const RemoteReindexRunning = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const { nextSteps, migrationStatus, handleTriggerStep } = useRemoteReindexMigrationStatus(currentStep, onTriggerStep);
  const indicesWithErrors = migrationStatus?.indices.filter((index) => index.status === 'ERROR') || [];

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
      <MigrationStepTriggerButtonToolbar nextSteps={nextSteps || currentStep.next_steps} onTriggerStep={handleTriggerStep} />
    </>
  );
};

export default RemoteReindexRunning;
