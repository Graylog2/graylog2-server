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

const DownsizeWarning = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const JournalDowntimeWarning = ({ nextSteps, onTriggerStep }: MigrationStepComponentProps) => (
  <>
    <h3>Journal downtime size warning</h3>
    <DownsizeWarning bsStyle="danger">
      <h4>During the next step the journal size will increase because of stopping the processing.</h4>
      <ul>
        <li>Current journal size: 1Go</li>
        <li>Messages: 10 000</li>
        <li>Volume size:  2Go</li>
        <li><b>Estimated down time: 5mn</b></li>
      </ul>
      <p>Please increase you journal volume size before proceeding.</p>
    </DownsizeWarning>
    <MigrationStepTriggerButtonToolbar nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
  </>
);
export default JournalDowntimeWarning;
