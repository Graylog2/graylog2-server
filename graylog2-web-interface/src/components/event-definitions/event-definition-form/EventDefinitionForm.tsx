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
import type { SyntheticEvent } from 'react';
import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import defaultTo from 'lodash/defaultTo';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';
import URI from 'urijs';
import QS from 'qs';

import { getPathnameWithoutId } from 'util/URLUtils';
import { Col, Row } from 'components/bootstrap';
import { Wizard } from 'components/common';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';
import type { EventDefinition, EventDefinitionFormControlsProps } from 'components/event-definitions/event-definitions-types';
import type User from 'logic/users/User';
import useHistory from 'routing/useHistory';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import EventDefinitionFormControls from 'components/event-definitions/event-definition-form/EventDefinitionFormControls';

import EventDetailsForm from './EventDetailsForm';
import EventConditionForm from './EventConditionForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

export const STEP_KEYS = ['event-details', 'condition', 'fields', 'notifications', 'summary'];
const STEP_TELEMETRY_KEYS = [
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_DETAILS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_CONDITION.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_FIELDS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_NOTIFICATIONS.STEP_CLICKED,
  TELEMETRY_EVENT_TYPE.EVENTDEFINITION_SUMMARY.STEP_CLICKED,
];

const getConditionPlugin = (type: string | undefined) => {
  if (type === undefined) {
    return { displayName: null };
  }

  return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
};

const WizardContainer = styled.div`
  margin-bottom: 10px;
`;

type Props = {
  activeStep: string,
  setActiveStep: React.Dispatch<React.SetStateAction<string>>
  action: 'edit' | 'create',
  eventDefinition: EventDefinition,
  currentUser: User,
  validation: {
    errors: {
      config?: unknown,
      title?: string,
    }
  },
  entityTypes: {},
  notifications: Array<EventNotification>,
  defaults: { default_backlog_size: number },
  onChange: (key: string, value: unknown) => void,
  onCancel: () => void,
  onSubmit: () => void
  canEdit: boolean,
  formControls?: React.ComponentType<EventDefinitionFormControlsProps>
}

const EventDefinitionForm = ({
  action,
  activeStep,
  canEdit,
  currentUser,
  defaults,
  entityTypes,
  eventDefinition,
  formControls: FormControls,
  notifications,
  onCancel,
  onChange,
  onSubmit,
  setActiveStep,
  validation,
}: Props) => {
  const history = useHistory();
  const { pathname } = useLocation();
  const sendTelemetry = useSendTelemetry();

  useEffect(() => {
    const currentUrl = new URI(window.location.href);
    const queryParameters = QS.parse(currentUrl.query());

    if (queryParameters.step !== activeStep) {
      const newUrl = currentUrl.removeSearch('step').addQuery('step', activeStep);
      history.replace(newUrl.resource());
    }
  }, [activeStep, history]);

  const handleSubmit = (event: SyntheticEvent) => {
    if (event) {
      event.preventDefault();
    }

    onSubmit();
  };

  const defaultStepProps = {
    key: eventDefinition.id, // Recreate components if ID changed
    action,
    entityTypes,
    eventDefinition,
    onChange,
    validation,
    currentUser,
  };

  const canEditCondition = React.useMemo(() => (
    canEdit || eventDefinition._scope.toUpperCase() === 'ILLUMINATE'
  ), [canEdit, eventDefinition._scope]);

  const eventDefinitionType = getConditionPlugin(eventDefinition.config.type);

  const steps = [
    {
      key: STEP_KEYS[0],
      title: 'Event Details',
      component: <EventDetailsForm {...defaultStepProps} canEdit={canEdit} />,
    },
    {
      key: STEP_KEYS[1],
      title: defaultTo(eventDefinitionType.displayName, 'Condition'),
      component: <EventConditionForm {...defaultStepProps} canEdit={canEditCondition} />,
    },
    {
      key: STEP_KEYS[2],
      title: 'Fields',
      component: <FieldsForm {...defaultStepProps} canEdit={canEdit} />,
    },
    {
      key: STEP_KEYS[3],
      title: 'Notifications',
      component: <NotificationsForm {...defaultStepProps} notifications={notifications} defaults={defaults} />,
    },
    {
      key: STEP_KEYS[4],
      title: 'Summary',
      component: (
        <EventDefinitionSummary eventDefinition={eventDefinition}
                                currentUser={currentUser}
                                notifications={notifications}
                                validation={validation} />
      ),
    },
  ];

  const handleStepChange = (nextStep) => {
    sendTelemetry(STEP_TELEMETRY_KEYS[STEP_KEYS.indexOf(nextStep)], {
      app_pathname: getPathnameWithoutId(pathname),
      app_section: (action === 'create') ? 'new-event-definition' : 'edit-event-definition',
      app_action_value: 'event-definition-step',
      current_step: steps[STEP_KEYS.indexOf(activeStep)].title,
    });

    setActiveStep(nextStep);
  };

  return (
    <Row>
      <Col md={12}>
        <WizardContainer>
          <Wizard steps={steps}
                  activeStep={activeStep}
                  onStepChange={handleStepChange}
                  horizontal
                  justified
                  containerClassName=""
                  hidePreviousNextButtons />
        </WizardContainer>
        <FormControls activeStep={activeStep}
                      setActiveStep={setActiveStep}
                      action={action}
                      steps={steps}
                      stepKeys={STEP_KEYS}
                      onSubmit={handleSubmit}
                      onCancel={onCancel} />
      </Col>
    </Row>
  );
};

EventDefinitionForm.propTypes = {
  action: PropTypes.oneOf(['create', 'edit']),
  eventDefinition: PropTypes.object.isRequired,
  currentUser: PropTypes.object.isRequired,
  validation: PropTypes.object.isRequired,
  entityTypes: PropTypes.object.isRequired,
  notifications: PropTypes.array.isRequired,
  defaults: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  onSubmit: PropTypes.func.isRequired,
};

EventDefinitionForm.defaultProps = {
  action: 'edit',
  formControls: EventDefinitionFormControls,
};

export default EventDefinitionForm;
