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
import {
  MIGRATION_STATE,
  REMOTE_REINDEXING_MIGRATION_STEPS,
} from 'components/datanode/Constants';
import type { MigrationActions, StepArgs, MigrationStateItem, MigrationStepComponentProps } from 'components/datanode/Types';

import Welcome from './remoteReindexing/Welcome';
import ExistingDataMigrationQuestion from './remoteReindexing/ExistingDataMigrationQuestion';
import RemoteReindexRunning from './remoteReindexing/RemoteReindexRunning';
import CertificatesProvisioning from './common/CertificatesProvisioning';
import MigrateExistingData from './remoteReindexing/MigrateExistingData';
import ShutdownClusterStep from './remoteReindexing/ShutdownClusterStep';
import MigrationStepsPanel from './common/MigrationStepsPanel';

const StyledTitle = styled.h3`
  margin-bottom: 10px;

  & > small {
    font-size: 80%;
  }
`;

const RemoteReindexingMigration = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const onStepComplete = async (step: MigrationActions, args: StepArgs = {}) => onTriggerStep(step, args);

  const renderStepComponent = (step: MigrationStateItem, hideActions: boolean) => {
    switch (step) {
      case MIGRATION_STATE.REMOTE_REINDEX_WELCOME_PAGE.key:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_RUNNING.key:
        return <CertificatesProvisioning currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION_PAGE.key:
        return <ExistingDataMigrationQuestion currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.MIGRATE_EXISTING_DATA.key:
        return <MigrateExistingData currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key:
        return <RemoteReindexRunning currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      case MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key:
        return <ShutdownClusterStep currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
      default:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} hideActions={hideActions} />;
    }
  };

  return (
    <Col>
      <StyledTitle>Remote reindexing migration</StyledTitle>
      <p>Follow these steps to migrate your existing OpenSearch 2.x or 1.3.x cluster to Data Nodes.</p>
      <MigrationStepsPanel currentStep={currentStep}
                           sortedMigrationSteps={REMOTE_REINDEXING_MIGRATION_STEPS}
                           renderStepComponent={renderStepComponent} />
    </Col>
  );
};

export default RemoteReindexingMigration;
