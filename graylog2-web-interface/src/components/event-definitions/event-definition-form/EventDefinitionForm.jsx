import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import styled from 'styled-components';

import { Button, ButtonToolbar, Col, Nav, Row } from 'components/graylog';
import { Wizard } from 'components/common';
import { PluginStore } from 'graylog-web-plugin/plugin';

import EventDetailsForm from './EventDetailsForm';
import EventConditionForm from './EventConditionForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

const NavStyles = styled(Nav)(({ theme }) => `
  &.nav {
    > li {
      border: 1px solid ${theme.color.gray[80]};
      border-left: 0;

      &:first-child {
        border-left: 1px solid ${theme.color.gray[80]};
        border-radius: 4px 0 0 4px;

        > a {
          border-radius: 4px 0 0 4px;
        }
      }

      &:last-child {
        border-radius: 0 4px 4px 0;

        > a {
          border-radius: 0 4px 4px 0;
        }
      }

      &:not(:last-child)::after {
        background-color: ${theme.color.gray[100]};
        border-color: ${theme.color.gray[80]};
        border-style: solid;
        border-width: 0 1px 1px 0;
        content: '';
        display: block;
        height: 15px;
        position: absolute;
        right: -1px;
        top: 50%;
        transform: translateY(-50%) translateX(50%) rotate(-45deg);
        width: 15px;
        z-index: 2;
      }

      &:hover::after {
        background-color: ${theme.color.gray[90]};
      }

      &.active::after {
        background-color: ${theme.color.global.link};
      }

      > a {
        border-radius: 0;
      }
    }
  }
`);

const STEP_KEYS = ['event-details', 'condition', 'fields', 'notifications', 'summary'];

class EventDefinitionForm extends React.Component {
  static propTypes = {
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

  static defaultProps = {
    action: 'edit',
  };

  constructor(props) {
    super(props);

    this.state = {
      activeStep: STEP_KEYS[0],
    };
  }

  // TODO: Add validation when step changes
  handleStepChange = (nextStep) => {
    this.setState({ activeStep: nextStep });
  };

  handleSubmit = (event) => {
    const { activeStep } = this.state;

    if (event) {
      event.preventDefault();
    }

    if (activeStep === lodash.last(STEP_KEYS)) {
      const { onSubmit } = this.props;
      onSubmit();
    }
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
  };

  renderButtons = (activeStep) => {
    if (activeStep === lodash.last(STEP_KEYS)) {
      const { onCancel } = this.props;
      return (
        <div className="pull-right">
          <ButtonToolbar>
            <Button onClick={onCancel}>Cancel</Button>
            <Button bsStyle="primary" onClick={this.handleSubmit}>Done</Button>
          </ButtonToolbar>
        </div>
      );
    }

    const activeStepIndex = STEP_KEYS.indexOf(activeStep);
    const previousStep = activeStepIndex > 0 ? STEP_KEYS[activeStepIndex - 1] : undefined;
    const nextStep = STEP_KEYS[activeStepIndex + 1];

    return (
      <div>
        <Button bsStyle="info"
                onClick={() => this.handleStepChange(previousStep)}
                disabled={activeStepIndex === 0}>
          Previous
        </Button>
        <div className="pull-right">
          <Button bsStyle="info"
                  onClick={() => this.handleStepChange(nextStep)}>
            Next
          </Button>
        </div>
      </div>
    );
  };

  render() {
    const {
      action,
      entityTypes,
      eventDefinition,
      notifications,
      onChange,
      validation,
      defaults,
      currentUser,
    } = this.props;
    const { activeStep } = this.state;

    const defaultStepProps = {
      key: eventDefinition.id, // Recreate components if ID changed
      action,
      entityTypes,
      eventDefinition,
      onChange,
      validation,
      currentUser,
    };

    const eventDefinitionType = this.getConditionPlugin(eventDefinition.config.type);

    const steps = [
      {
        key: STEP_KEYS[0],
        title: 'Event Details',
        component: <EventDetailsForm {...defaultStepProps} />,
      },
      {
        key: STEP_KEYS[1],
        title: lodash.defaultTo(eventDefinitionType.displayName, 'Condition'),
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
          <EventDefinitionSummary action={action}
                                  eventDefinition={eventDefinition}
                                  currentUser={currentUser}
                                  notifications={notifications}
                                  validation={validation} />
        ),
      },
    ];

    return (
      <Row>
        <Col md={12}>
          <Wizard steps={steps}
                  activeStep={activeStep}
                  onStepChange={this.handleStepChange}
                  horizontal
                  justified
                  NavigationComponent={NavStyles}
                  containerClassName=""
                  hidePreviousNextButtons />
          {this.renderButtons(activeStep)}
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionForm;
