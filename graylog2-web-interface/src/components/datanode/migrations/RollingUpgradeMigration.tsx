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

import { Col, Panel, PanelGroup } from 'components/bootstrap';
import { MIGRATION_STATE, ROLLING_UPGRADE_MIGRATION_STEPS } from 'components/datanode/Constants';
import type { MigrationActions, MigrationState, StepArgs } from 'components/datanode/Types';
import Welcome from 'components/datanode/migrations/rollingUpgrade/Welcome';
import CertificatesProvisioning from 'components/datanode/migrations/rollingUpgrade/CertificatesProvisioning';
import JournalDowntimeWarning from 'components/datanode/migrations/rollingUpgrade/JournalDowntimeWarning';
// import ConnectionStringRemovalStep from 'components/datanode/migrations/ConnectionStringRemovalStep';
import MigrateActions from 'components/datanode/migrations/rollingUpgrade/MigrateActions';
import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
// import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';

type Props = {
    currentStep: MigrationState,
    onTriggerNextStep: (step: MigrationActions, args: StepArgs) => void,
}
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

const RollingUpgradeMigration = ({ currentStep, onTriggerNextStep }: Props) => {
  const { next_steps: nextSteps, state: activeStep } = currentStep;

  const onStepComplete = (step: MigrationActions, args: StepArgs = {}) => {
    onTriggerNextStep(step, args);
  };

  const steps = [
    {
      key: MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key,
      component: <Welcome nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE2.key,
      component: <CompatibilityCheckStep nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key,
      component: <CertificatesProvisioning nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key,
      component: <CertificatesProvisioning nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key,
      component: <JournalDowntimeWarning nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
    {
      key: MIGRATION_STATE.MESSAGE_PROCESSING_STOP_REPLACE_CLUSTER_AND_MP_RESTART.key,
      component: <MigrateActions nextSteps={nextSteps} onTriggerStep={onStepComplete} />,
    },
  ];

  return (
    <Col>
      <StyledTitle>Rolling upgrade migration.</StyledTitle>
      <p>Follow these steps to migrate your existing migrating an existing OpenSearch 2.x or 1.3.x cluster to Data
        Node
      </p>
      <StyledPanelGroup accordion id="first" activeKey={activeStep} onSelect={() => {}}>
        {ROLLING_UPGRADE_MIGRATION_STEPS.map((rollingUpgradeStep, index) => {
          const { description } = MIGRATION_STATE[rollingUpgradeStep];

          return (
            <Panel eventKey={rollingUpgradeStep} key={rollingUpgradeStep} collapsible={false}>
              <Panel.Heading>
                <Panel.Title>
                  <Panel.Toggle tabIndex={index}>{`${index + 1}. ${description}`}</Panel.Toggle>
                </Panel.Title>
              </Panel.Heading>
              <Panel.Collapse>
                <Panel.Body>
                  {steps.filter((step) => (step.key === rollingUpgradeStep))
                    .map((item) => item)[0]?.component}
                </Panel.Body>
              </Panel.Collapse>
            </Panel>
          );
        })}

      </StyledPanelGroup>
    </Col>
  );
};

export default RollingUpgradeMigration;
