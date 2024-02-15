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

import useRemoteReindexMigrationStatus from '../../hooks/useRemoteReindexMigrationStatus';
import type { MigrationStepComponentProps } from '../../Types';
import MigrationStepTriggerButtonToolbar from '../common/MigrationStepTriggerButtonToolbar';

const RemoteReindexRunning = ({ nextSteps, onTriggerStep }: MigrationStepComponentProps) => {
  const migrationID = '';
  const roundedPercentage = 50;

  const onSuccess = () => {
    onTriggerStep(nextSteps[0], {});
  };

  const { data, error } = useRemoteReindexMigrationStatus(migrationID, onSuccess);
  console.log(data, error);

  return (
    <>
      We are currently migrating your existing data asynchronically,
      once the data migration is finished you will be automatically transitioned to the next step.
      <br />
      <br />
      <ProgressBar bars={[{
        animated: true,
        striped: true,
        value: roundedPercentage,
        bsStyle: 'info',
        label: data?.status,
      }]} />
      <MigrationStepTriggerButtonToolbar hidden nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default RemoteReindexRunning;
