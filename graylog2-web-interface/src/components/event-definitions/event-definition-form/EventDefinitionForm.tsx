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
import type { SyntheticEvent } from 'react';
import styled from 'styled-components';

import { getPathnameWithoutId } from 'util/URLUtils';
import { Col, Row } from 'components/bootstrap';
import { Wizard } from 'components/common';
import type { StepType } from 'components/common/Wizard';
import type { EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import EventDefinitionFormControls from 'components/event-definitions/event-definition-form/EventDefinitionFormControls';

const WizardContainer = styled.div`
  margin-bottom: 10px;
`;

const STEP_KEYS = {
  EVENT_DETAILS: 'event-details',
  CONDITION: 'condition',
  ADDITIONAL_DETAILS: 'additional-details',
  NOTIFICATIONS: 'notifications',
  SHARE: 'Share',
  SUMMARY: 'summary',
};

export const getStepKeys = (isNew: boolean, hideFieldsStep = false) => [
  STEP_KEYS.EVENT_DETAILS,
  STEP_KEYS.CONDITION,
  ...(hideFieldsStep ? [] : [STEP_KEYS.ADDITIONAL_DETAILS]),
  STEP_KEYS.NOTIFICATIONS,
  ...(isNew ? [STEP_KEYS.SHARE] : []),
  STEP_KEYS.SUMMARY,
];

// Maps legacy `?step=` query-param values to their current keys so older bookmarked
// links keep landing on the right step (e.g. `fields` was renamed to `additional-details`).
const LEGACY_STEP_KEYS: Record<string, string> = {
  fields: STEP_KEYS.ADDITIONAL_DETAILS,
};

export const normalizeStepKey = (step: string | undefined): string | undefined =>
  step ? (LEGACY_STEP_KEYS[step] ?? step) : step;

const STEP_TELEMETRY_KEYS = [
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DETAILS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.STEP_CLICKED,
];

type Props = {
  steps: Array<StepType<string>>;
  activeStep: string;
  action?: 'edit' | 'create';
  onChangeStep: (step: string) => void;
  onCancel: () => void;
  onSubmit: () => void;
  formControls?: React.ComponentType<EventDefinitionFormControlsProps>;
};

const EventDefinitionForm = ({
  steps,
  action = 'edit',
  activeStep,
  formControls: FormControls = EventDefinitionFormControls,
  onCancel,
  onChangeStep,
  onSubmit,
}: Props) => {
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  const handleSubmit = (event: SyntheticEvent) => {
    if (event) {
      event.preventDefault();
    }

    onSubmit();
  };

  const currentStepKeys = steps.map((step) => step.key);
  const activeStepIndex = currentStepKeys.indexOf(activeStep);

  const handleStepChange = (nextStep: string) => {
    sendTelemetry(STEP_TELEMETRY_KEYS[currentStepKeys.indexOf(nextStep)], {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: action === 'create' ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'event-definition-step',
      current_step: steps[activeStepIndex]?.title,
    });

    onChangeStep(nextStep);
  };

  const openPrevPage = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_PREVIOUS_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: action === 'create' ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'previous-button',
      current_step: steps[activeStepIndex]?.title,
    });

    const previousStep = activeStepIndex > 0 ? currentStepKeys[activeStepIndex - 1] : undefined;
    onChangeStep(previousStep);
  };

  const openNextPage = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NEXT_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: action === 'create' ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'next-button',
      current_step: steps[activeStepIndex]?.title,
    });

    const nextStep = currentStepKeys[activeStepIndex + 1];
    onChangeStep(nextStep);
  };

  return (
    <Row>
      <Col md={12}>
        <WizardContainer>
          <Wizard
            steps={steps}
            activeStep={activeStep}
            onStepChange={handleStepChange}
            horizontal
            justified
            containerClassName=""
            hidePreviousNextButtons
          />
        </WizardContainer>
        <FormControls
          activeStepIndex={activeStepIndex}
          action={action}
          onOpenPrevPage={openPrevPage}
          onOpenNextPage={openNextPage}
          steps={steps}
          onSubmit={handleSubmit}
          onCancel={onCancel}
        />
      </Col>
    </Row>
  );
};

export default EventDefinitionForm;
