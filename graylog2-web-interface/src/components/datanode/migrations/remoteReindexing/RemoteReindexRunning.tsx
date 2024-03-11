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

import { ProgressBar } from 'components/common';

import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';
import useRemoteReindexMigrationStatus from '../../hooks/useRemoteReindexMigrationStatus';

const RemoteReindexRunning = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const { nextSteps, migrationStatus } = useRemoteReindexMigrationStatus(currentStep, onTriggerStep);

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
      <MigrationStepTriggerButtonToolbar nextSteps={nextSteps || currentStep.next_steps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default RemoteReindexRunning;
