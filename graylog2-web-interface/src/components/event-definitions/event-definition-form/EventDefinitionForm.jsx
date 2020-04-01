import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, ButtonToolbar, Col, Row } from 'components/graylog';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Wizard } from 'components/common';
import EventDetailsForm from './EventDetailsForm';
import EventConditionForm from './EventConditionForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

import styles from './EventDefinitionForm.css';

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

  state = {
    activeStep: STEP_KEYS[0],
  };

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
                  navigationClassName={styles.steps}
                  containerClassName=""
                  hidePreviousNextButtons />
          {this.renderButtons(activeStep)}
        </Col>
      </Row>
    );
  }
}

export default EventDefinitionForm;
