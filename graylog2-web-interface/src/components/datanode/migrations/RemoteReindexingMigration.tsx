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
import type { MigrationActions, StepArgs, MigrationStateItem, MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

import Welcome from './remoteReindexing/Welcome';
import ExistingDataMigrationQuestion from './remoteReindexing/ExistingDataMigrationQuestion';
import RemoteReindexRunning from './remoteReindexing/RemoteReindexRunning';
import CertificatesProvisioning from './common/CertificatesProvisioning';
import MigrateExistingData from './remoteReindexing/MigrateExistingData';
import ShutdownClusterStep from './remoteReindexing/ShutdownClusterStep';
import ConnectionStringRemovalStep from './remoteReindexing/ConnectionStringRemovalStep';

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
      background-color: ${(props) => props.theme.colors.table.row.backgroundAlt};
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

const RemoteReindexingMigration = ({ currentStep, onTriggerStep }: MigrationStepComponentProps) => {
  const { state: activeStep } = currentStep;

  const onStepComplete = async (step: MigrationActions, args: StepArgs = {}) => onTriggerStep(step, args);

  const getStepComponent = (step: MigrationStateItem) => {
    switch (step) {
      case MIGRATION_STATE.REMOTE_REINDEX_WELCOME_PAGE.key:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_PAGE.key:
      case MIGRATION_STATE.PROVISION_DATANODE_CERTIFICATES_RUNNING.key:
        return <CertificatesProvisioning currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.MANUALLY_REMOVE_OLD_CONNECTION_STRING_FROM_CONFIG.key:
        return <ConnectionStringRemovalStep currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.EXISTING_DATA_MIGRATION_QUESTION_PAGE.key:
        return <ExistingDataMigrationQuestion currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.MIGRATE_EXISTING_DATA.key:
        return <MigrateExistingData currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key:
        return <RemoteReindexRunning currentStep={currentStep} onTriggerStep={onStepComplete} />;
      case MIGRATION_STATE.ASK_TO_SHUTDOWN_OLD_CLUSTER.key:
        return <ShutdownClusterStep currentStep={currentStep} onTriggerStep={onStepComplete} />;
      default:
        return <Welcome currentStep={currentStep} onTriggerStep={onStepComplete} />;
    }
  };

  return (
    <Col>
      <StyledTitle>Remote reindexing migration</StyledTitle>
      <p>Follow these steps to migrate your existing OpenSearch 2.x or 1.3.x cluster to Data Nodes.</p>
      <StyledPanelGroup accordion id="first" activeKey={activeStep} onSelect={() => {}}>
        {REMOTE_REINDEXING_MIGRATION_STEPS.map((remoteReindexingStep, index) => {
          const { description } = MIGRATION_STATE[remoteReindexingStep];

          return (
            <Panel key={remoteReindexingStep} eventKey={remoteReindexingStep} collapsible={false}>
              <Panel.Heading>
                <Panel.Title>
                  <Panel.Toggle tabIndex={index}>{`${index + 1}. ${description}`}</Panel.Toggle>
                </Panel.Title>
              </Panel.Heading>
              <Panel.Body collapsible>
                <MigrationError errorMessage={currentStep.error_message} />
                {getStepComponent(remoteReindexingStep)}
              </Panel.Body>
            </Panel>
          );
        })}
      </StyledPanelGroup>
    </Col>
  );
};

export default RemoteReindexingMigration;
