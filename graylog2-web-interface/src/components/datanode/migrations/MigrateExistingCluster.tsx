// @ts-nocheck
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
import React, { useEffect, useState } from 'react';
import styled from 'styled-components';

import { Col, Panel, PanelGroup } from 'components/bootstrap';
import { MIGRATION_STATE, ROLLING_UPGRADE_MIGRATION_STEPS } from 'components/datanode/Constants';
import type { MigrationActions, MigrationState } from 'components/datanode/Types';
import Welcome from 'components/datanode/migrations/rollingUpgrade/Welcome';
import DirectoryCompatibilityCheck from 'components/datanode/migrations/rollingUpgrade/DirectoryCompatibilityCheck';
import CertificatesProvisioning from 'components/datanode/migrations/rollingUpgrade/CertificatesProvisioning';
import JournalDowntimeWarning from 'components/datanode/migrations/rollingUpgrade/JournalDowntimeWarning';
import ConnectionStringRemoval from 'components/datanode/migrations/rollingUpgrade/ConnectionStringRemoval';
import MigrationFinished from 'components/datanode/migrations/rollingUpgrade/MigrationFinished';
import MigrateActions from 'components/datanode/migrations/rollingUpgrade/MigrateActions';

type Props = {
    currentStep: MigrationState,
    onTriggerNextStep: (step:{step: MigrationActions}) => void,
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

const MigrateExistingCluster = ({ currentStep, onTriggerNextStep }: Props) => {
  const [activeStep, setActiveStep] = useState(currentStep.state);

  useEffect(() => {
    setActiveStep(currentStep.state);
  }, [currentStep]);

  const steps = [
    {
      key: MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME.key,
      component: <Welcome onStepComplete={onTriggerNextStep} nextSteps={currentStep.next_steps} />,
    },
    {
      key: MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key,
      component: <DirectoryCompatibilityCheck onStepComplete={onTriggerNextStep} nextSteps={currentStep.next_steps} />,
    },
    {
      key: MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_WITH_CERTIFICATES.key,
      component: <CertificatesProvisioning onStepComplete={onTriggerNextStep} nextSteps={currentStep.next_steps} />,
    },
    {
      key: MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key,
      component: <JournalDowntimeWarning onStepComplete={() => setActiveStep('MIGRATE')} nextSteps={currentStep.next_steps} />,
    },
    {
      key: MIGRATION_STATE.MIGRATE.key,
      component: <MigrateActions onStepComplete={onTriggerNextStep} nextSteps={['CONFIRM_OLD_CLUSTER_STOPPED']} />,
    },
    {
      key: MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key,
      component: <ConnectionStringRemoval onStepComplete={onTriggerNextStep} nextSteps={currentStep.next_steps} />,
    }, {
      key: MIGRATION_STATE.FINISHED.key,
      component: <MigrationFinished />,
    },
  ];

  return (
    <Col>
      <StyledTitle>Rolling upgrade migration.</StyledTitle>
      <p>Follow these steps to migrate your existing migrating an existing OpenSearch 2.x or 1.3.x cluster to Data
        Node
      </p>
      <StyledPanelGroup accordion id="first" activeKey={activeStep}>
        {ROLLING_UPGRADE_MIGRATION_STEPS.map((rollingUpgradeStep, index) => {
          const { description } = MIGRATION_STATE[rollingUpgradeStep];

          return (
            <Panel eventKey={rollingUpgradeStep} key={rollingUpgradeStep} collapsible={false}>
              <Panel.Heading>
                <Panel.Title>
                  <Panel.Toggle tabIndex={index}>{`${index}. ${description}`}</Panel.Toggle>
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

export default MigrateExistingCluster;
