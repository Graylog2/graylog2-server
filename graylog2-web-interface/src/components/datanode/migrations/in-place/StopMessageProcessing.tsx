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

import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import { Panel } from 'components/bootstrap';
import { Icon } from 'components/common';
import { StyledPanel } from 'components/datanode/migrations/MigrationWelcomeStep';
import useProductName from 'brand-customization/useProductName';

const StyledHelpPanel = styled(StyledPanel)`
  margin-top: 30px;
`;

const StopMessageProcessing = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const productName = useProductName();

  return (
    <>
      <p>{productName} processing is stopped.</p>
      <StyledHelpPanel bsStyle="warning">
        <Panel.Heading>
          <Panel.Title componentClass="h3">
            <Icon name="warning" />
            Stop OpenSearch
          </Panel.Title>
        </Panel.Heading>
        <Panel.Body>
          <p>Please stop your OpenSearch cluster before proceeding.</p>
          <p>
            If you are migrating existing OpenSearch data by pointing the data node to its data directory, make sure to
            change the owner of the data directory to the user running the data node (usually graylog-datanode) and
            reset the correct permissions, e.g. by running
          </p>
          <p>
            <code>sudo chown -R graylog-datanode:graylog-datanode &lt;your_data_directory&gt;</code>
            <br />
            <code>sudo chmod -R 750 &lt;your_data_directory&gt;</code>
          </p>
        </Panel.Body>
      </StyledHelpPanel>
      <p />
      <MigrationStepTriggerButtonToolbar
        hidden={hideActions}
        nextSteps={currentStep.next_steps}
        onTriggerStep={onTriggerStep}
      />
    </>
  );
};

export default StopMessageProcessing;
