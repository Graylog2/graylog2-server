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
import JournalSizeWarning from 'components/datanode/migrations/rollingUpgrade/JournalSizeWarning';

const DownsizeWarning = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const JournalDowntimeWarning = ({ nextSteps, onTriggerStep }: MigrationStepComponentProps) => (
  <>
    <h3>Journal downtime size warning</h3>
    <JournalSizeWarning />
    <DownsizeWarning bsStyle="danger">
      <p>Please make sure your journal volume size is enough before proceeding.</p>
    </DownsizeWarning>
    <MigrationStepTriggerButtonToolbar nextSteps={nextSteps} onTriggerStep={onTriggerStep} />
  </>
);
export default JournalDowntimeWarning;
