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
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import last from 'lodash/last';
import defaultTo from 'lodash/defaultTo';
import { PluginStore } from 'graylog-web-plugin/plugin';
import styled from 'styled-components';

import { Button, Col, Row } from 'components/bootstrap';
import { ModalSubmit, Wizard } from 'components/common';
import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type User from 'logic/users/User';
import useQuery from 'routing/useQuery';

import EventDetailsForm from './EventDetailsForm';
import EventConditionForm from './EventConditionForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

const STEP_KEYS = ['event-details', 'condition', 'fields', 'notifications', 'summary'];

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
}

const EventDefinitionForm = ({
  action,
  eventDefinition,
  currentUser,
  validation,
  entityTypes,
  notifications,
  defaults,
  onChange,
  onCancel,
  onSubmit,
}: Props) => {
  const { step } = useQuery();
  const [activeStep, setActiveStep] = useState(step as string || STEP_KEYS[0]);

  const handleStepChange = (nextStep) => {
    setActiveStep(nextStep);
  };

  const handleSubmit = (event: SyntheticEvent) => {
    if (event) {
      event.preventDefault();
    }

    if (activeStep === last(STEP_KEYS)) {
      onSubmit();
    }
  };

  const renderButtons = () => {
    if (activeStep === last(STEP_KEYS)) {
      return (
        <ModalSubmit onCancel={onCancel}
                     onSubmit={handleSubmit}
                     submitButtonText={`${eventDefinition.id ? 'Update' : 'Create'} event definition`} />
      );
    }

    const activeStepIndex = STEP_KEYS.indexOf(activeStep);
    const previousStep = activeStepIndex > 0 ? STEP_KEYS[activeStepIndex - 1] : undefined;
    const nextStep = STEP_KEYS[activeStepIndex + 1];

    return (
      <div>
        <Button bsStyle="info"
                onClick={() => handleStepChange(previousStep)}
                disabled={activeStepIndex === 0}>
          Previous
        </Button>
        <div className="pull-right">
          <Button bsStyle="info"
                  onClick={() => handleStepChange(nextStep)}>
            Next
          </Button>
        </div>
      </div>
    );
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

  const eventDefinitionType = getConditionPlugin(eventDefinition.config.type);

  const steps = [
    {
      key: STEP_KEYS[0],
      title: 'Event Details',
      component: <EventDetailsForm {...defaultStepProps} />,
    },
    {
      key: STEP_KEYS[1],
      title: defaultTo(eventDefinitionType.displayName, 'Condition'),
      component: <EventConditionForm {...defaultStepProps} />,
    },
    {
      key: STEP_KEYS[2],
      title: 'Fields',
      component: <FieldsForm {...defaultStepProps} />,
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
        {renderButtons()}
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
};

export default EventDefinitionForm;
