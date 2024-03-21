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
import last from 'lodash/last';

import { ModalSubmit } from 'components/common';
import { Button } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { getPathnameWithoutId } from 'util/URLUtils';
import useLocation from 'routing/useLocation';
import type { EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';

const EventDefinitionFormControls = ({
  activeStep,
  onChangeStep,
  onSubmit,
  onCancel,
  stepKeys,
  action,
  steps,
}: EventDefinitionFormControlsProps) => {
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();

  if (activeStep === last(stepKeys)) {
    return (
      <ModalSubmit onCancel={onCancel}
                   onSubmit={onSubmit}
                   submitButtonText={`${action === 'edit' ? 'Update' : 'Create'} event definition`} />
    );
  }

  const activeStepIndex = stepKeys.indexOf(activeStep);

  const handlePreviousClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_PREVIOUS_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: (action === 'create') ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'previous-button',
      current_step: steps[activeStepIndex].title,
    });

    const previousStep = activeStepIndex > 0 ? stepKeys[activeStepIndex - 1] : undefined;
    onChangeStep(previousStep);
  };

  const handleNextClick = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NEXT_CLICKED, {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: (action === 'create') ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'next-button',
      current_step: steps[activeStepIndex].title,
    });

    const nextStep = stepKeys[activeStepIndex + 1];
    onChangeStep(nextStep);
  };

  return (
    <div>
      <Button bsStyle="info"
              onClick={handlePreviousClick}
              disabled={activeStepIndex === 0}>
        Previous
      </Button>
      <div className="pull-right">
        <Button bsStyle="info"
                onClick={handleNextClick}>
          Next
        </Button>
      </div>
    </div>
  );
};

export default EventDefinitionFormControls;
