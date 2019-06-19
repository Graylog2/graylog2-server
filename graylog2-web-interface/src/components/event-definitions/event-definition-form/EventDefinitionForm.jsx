import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Button, ButtonToolbar, Col, Row } from 'react-bootstrap';

import { Wizard } from 'components/common';
import EventDetailsForm from './EventDetailsForm';
import FilterAggregationForm from './FilterAggregationForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

import styles from './EventDefinitionForm.css';

const STEP_KEYS = ['event-details', 'filter-aggregation', 'fields', 'notifications', 'summary'];

class EventDefinitionForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object.isRequired,
    allFieldTypes: PropTypes.array.isRequired,
    streams: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    streams: [],
  };

  state = {
    activeStep: STEP_KEYS[0],
  };

  // TODO: Add validation when step changes
  handleStepChange = (nextStep) => {
    this.setState({ activeStep: nextStep });
  };

  handleSubmit = (event) => {
    if (event) {
      event.preventDefault();
    }
    const { onSubmit } = this.props;
    onSubmit();
  };

  renderButtons = (activeStep) => {
    if (activeStep === lodash.last(STEP_KEYS)) {
      const { onCancel } = this.props;
      return (
        <Row>
          <Col md={2} mdOffset={7}>
            <ButtonToolbar>
              <Button bsStyle="primary" onClick={this.handleSubmit}>Done</Button>
              <Button onClick={onCancel}>Cancel</Button>
            </ButtonToolbar>
          </Col>
        </Row>
      );
    }
    return null;
  };

  render() {
    const { action, allFieldTypes, eventDefinition, streams, onChange } = this.props;
    const { activeStep } = this.state;

    const defaultStepProps = {
      action,
      eventDefinition,
      onChange,
    };

    const steps = [
      {
        key: STEP_KEYS[0],
        title: 'Event Details',
        component: <EventDetailsForm {...defaultStepProps} />,
      },
      {
        key: STEP_KEYS[1],
        title: 'Filter & Aggregation',
        component: <FilterAggregationForm {...defaultStepProps} allFieldTypes={allFieldTypes} streams={streams} />,
      },
      {
        key: STEP_KEYS[2],
        title: 'Fields',
        component: <FieldsForm {...defaultStepProps} />,
      },
      {
        key: STEP_KEYS[3],
        title: 'Notifications',
        component: <NotificationsForm {...defaultStepProps} />,
      },
      {
        key: STEP_KEYS[4],
        title: 'Summary',
        component: <EventDefinitionSummary action={action} eventDefinition={eventDefinition} />,
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
