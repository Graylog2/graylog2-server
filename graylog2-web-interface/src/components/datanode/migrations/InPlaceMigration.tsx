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

import { Col } from 'components/bootstrap';
import type { MigrationActions, StepArgs, MigrationStateItem, MigrationStepComponentProps } from 'components/datanode/Types';
import { IN_PLACE_MIGRATION_STEPS, MIGRATION_STATE } from 'components/datanode/Constants';
import Welcome from 'components/datanode/migrations/in-place/Welcome';
import CertificatesProvisioning from 'components/datanode/migrations/common/CertificatesProvisioning';
import JournalDowntimeWarning from 'components/datanode/migrations/in-place/JournalDowntimeWarning';
import StopMessageProcessing from 'components/datanode/migrations/in-place/StopMessageProcessing';
import CompatibilityCheckStep from 'components/datanode/migrations/CompatibilityCheckStep';
import RestartGraylog from 'components/datanode/migrations/in-place/RestartGraylog';

import MigrationStepsPanel from './common/MigrationStepsPanel';

const StyledTitle = styled.h3`
  margin-bottom: 10px;

  & > small {
    font-size: 80%;
  }
`;

const InPlaceMigration = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const onStepComplete = async (step: MigrationActions, args: StepArgs = {}) => onTriggerStep(step, args);

  const renderStepComponent = (step: MigrationStateItem, hideActions: boolean) => {
    switch (step) {
      case MIGRATION_STATE.ROLLING_UPGRADE_MIGRATION_WELCOME_PAGE.key:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.DIRECTORY_COMPATIBILITY_CHECK_PAGE.key:
        return <CompatibilityCheckStep currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.PROVISION_ROLLING_UPGRADE_NODES_RUNNING.key:
        return <CertificatesProvisioning currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.JOURNAL_SIZE_DOWNTIME_WARNING.key:
        return <JournalDowntimeWarning currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.MESSAGE_PROCESSING_STOP.key:
        return <StopMessageProcessing currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.RESTART_GRAYLOG.key:
        return <RestartGraylog currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      default:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
    }
  };

  return (
    <Col>
      <StyledTitle>In-Place migration</StyledTitle>
      <p>Follow these steps to migrate your existing OpenSearch 2.x or 1.3.x cluster to Data Nodes.</p>
      <MigrationStepsPanel currentStep={currentStep}
                           sortedMigrationSteps={IN_PLACE_MIGRATION_STEPS}
                           renderStepComponent={renderStepComponent} />
    </Col>
  );
};

export default InPlaceMigration;
