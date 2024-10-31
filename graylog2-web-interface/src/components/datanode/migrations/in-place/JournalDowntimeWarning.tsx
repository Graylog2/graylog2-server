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

import { Alert } from 'components/bootstrap';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';

import useJournalDowntimeSize from '../../hooks/useJournalDowntimeSize';
import MigrationError from '../common/MigrationError';

const DownsizeWarning = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const JournalDowntimeWarning = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { data, error, isError } = useJournalDowntimeSize();

  return (
    <>
      <h3>Journal downtime size warning</h3>
      <p>Please note that during migration data processing  will stop on your Graylog node, this will result in the journal growing in size.</p>
      <p>Therefore you might need to increase your journal volume size.</p>
      <p>Your current journal size is: <b>{data.journal_size_MB} MB</b> and your current journal throughput is: <b>{data.KBs_per_minute} KB/min</b></p>
      <p>Your current maximum downtime for reconfiguring Graylog to point to the data node is: <b>{data.max_downtime_duration}</b></p>
      {isError && (
        <MigrationError errorMessage={`There was an error while estimating your journal throughput: ${error?.message}`} />
      )}
      <DownsizeWarning bsStyle="warning">
        Please make sure your journal volume size is sufficient before proceeding.
      </DownsizeWarning>
      <MigrationStepTriggerButtonToolbar hidden={hideActions} nextSteps={currentStep.next_steps} onTriggerStep={onTriggerStep} />
    </>
  );
};

export default JournalDowntimeWarning;
