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
import React from 'react';
import styled from 'styled-components';

import { Col, Panel, PanelGroup } from 'components/bootstrap';
import {
  MIGRATION_STATE,
  REMOTE_REINDEXING_MIGRATION_STEPS,
} from 'components/datanode/Constants';
import type { MigrationActions, StepArgs, MigrationState } from 'components/datanode/Types';

import Welcome from './remoteReindexing/Welcome';
import ExistingDataMigrationQuestion from './remoteReindexing/ExistingDataMigrationQuestion';
import MigrationWithDowntimeQuestion from './remoteReindexing/MigrationWithDowntimeQuestion';
import CertificatesProvisioning from './rollingUpgrade/CertificatesProvisioning';

const StyledTitle = styled.h3`
  margin-bottom: 10px;

  & > small {
    font-size: 80%;
  }
`;
const StyledPanelGroup = styled(PanelGroup)`
  &.panel-group > .panel {
    margin-top: 0;
    border-color: ${(props) => props.theme.colors.input.border};
    background-color: ${(props) => props.theme.colors.global.contentBackground};

    .panel-heading {
      background-color: ${(props) => props.theme.colors.table.backgroundAlt};
    }

    &:not(:first-child) {
      border-top: 0;
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }

    &:not(:last-child) {
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }
  }
`;

type Props = {
  currentStep: MigrationState,
  onTriggerNextStep: (step: MigrationActions, args: StepArgs) => void,
}

const RemoteReindexingMigration = ({ currentStep, onTriggerNextStep }: Props) => {
  const { next_steps: nextSteps, state: activeStep } = currentStep;

  const onStepComplete = (step: MigrationActions, args: StepArgs = {}) => {
    onTriggerNextStep(step, args);
  };

  const steps = [
    {
      key: MIGRATION_STATE.REMOTE_REINDEX_WELCOME.key,
      component: <Welcome nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES.key,
      component: <CertificatesProvisioning nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION.key,
      component: <ExistingDataMigrationQuestion nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.MIGRATE_WITH_DOWNTIME_QUESTION.key,
      component: <MigrationWithDowntimeQuestion nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
  ];

  return (
    <Col md={6}>
      <StyledTitle>Remote reindexing migration.</StyledTitle>
      <p>Follow these steps to migrate your existing migrating an existing OpenSearch 2.x or 1.3.x cluster to Data
        Node.
      </p>
      <StyledPanelGroup accordion id="first" activeKey={activeStep}>
        {REMOTE_REINDEXING_MIGRATION_STEPS.map((remoteReindexingStep, index) => {
          const { description } = MIGRATION_STATE[remoteReindexingStep];

          return (
            <Panel key={remoteReindexingStep} eventKey={remoteReindexingStep}>
              <Panel.Heading>
                <Panel.Title>
                  <Panel.Toggle tabIndex={index}>{`${index}. ${description}`}</Panel.Toggle>
                </Panel.Title>
              </Panel.Heading>
              <Panel.Body collapsible>
                {steps.filter((step) => (step.key === remoteReindexingStep))
                  .map((item) => item)[0]?.component}
              </Panel.Body>
            </Panel>
          );
        })}

      </StyledPanelGroup>
    </Col>
  );
};

export default RemoteReindexingMigration;
