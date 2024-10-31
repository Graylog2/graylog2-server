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
import styled, { css } from 'styled-components';

import { Panel, PanelGroup } from 'components/bootstrap';
import type { MigrationState, MigrationStateItem } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';

import { MIGRATION_STATE } from '../../Constants';

const StyledPanelGroup = styled(PanelGroup)`
  &.panel-group > .panel {
    margin-top: 0;
    border-color: ${(props) => props.theme.colors.input.border};
    background-color: ${(props) => props.theme.colors.global.contentBackground};

    .panel-heading {
      background-color: ${(props) => props.theme.colors.table.row.backgroundStriped};
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

const PanelToggle: React.ComponentType<React.ComponentProps<typeof Panel.Toggle> & { $clickable: boolean }> = styled(Panel.Toggle)(({ $clickable = true }) => css`
  cursor: ${$clickable ? 'pointer' : 'none'};
  pointer-events: ${$clickable ? 'auto' : 'none'};
`);

const PanelBody: React.ComponentType<React.ComponentProps<typeof Panel.Body> & { $editable: boolean }> = styled(Panel.Body)(({ $editable = false }) => css`
  cursor: ${$editable ? 'inherit' : 'none'};
  pointer-events: ${$editable ? 'inherit' : 'none'};
  background-color: ${$editable ? 'inherit' : (props) => props.theme.colors.global.navigationBoxShadow};
`);

type Props = {
  currentStep: MigrationState,
  sortedMigrationSteps: MigrationStateItem[],
  renderStepComponent: (step: MigrationStateItem, hideActions: boolean) => JSX.Element
}

const MigrationStepsPanel = ({ currentStep, sortedMigrationSteps, renderStepComponent }: Props) => {
  const { state: activeStep } = currentStep;
  const activeStepIndex = sortedMigrationSteps.indexOf(activeStep);

  return (
    <StyledPanelGroup accordion={false} id="first" activeKey={activeStep} onSelect={() => {}}>
      {sortedMigrationSteps.map((migrationStep, index) => {
        const { description } = MIGRATION_STATE[migrationStep];
        const isCurrentStep = migrationStep === activeStep;
        const isPreviousStep = index < activeStepIndex;

        return (
          <Panel key={migrationStep} eventKey={migrationStep} collapsible={false}>
            <Panel.Heading>
              <Panel.Title>
                <PanelToggle tabIndex={index} $clickable={isPreviousStep}>{`${index + 1}. ${description}`}</PanelToggle>
              </Panel.Title>
            </Panel.Heading>
            <PanelBody collapsible={!isCurrentStep} $editable={isCurrentStep}>
              <MigrationError errorMessage={currentStep.error_message} />
              {renderStepComponent(migrationStep, !isCurrentStep)}
            </PanelBody>
          </Panel>
        );
      })}
    </StyledPanelGroup>
  );
};

export default MigrationStepsPanel;
