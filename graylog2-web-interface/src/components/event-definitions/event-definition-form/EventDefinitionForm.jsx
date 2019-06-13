import React from 'react';
import PropTypes from 'prop-types';

import { Wizard } from 'components/common';
import EventDetailsForm from './EventDetailsForm';
import FilterAggregationForm from './FilterAggregationForm';
import FieldsForm from './FieldsForm';
import NotificationsForm from './NotificationsForm';
import EventDefinitionSummary from './EventDefinitionSummary';

import styles from './EventDefinitionForm.css';

class EventDefinitionForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    streams: [],
  };

  handleSubmit = (event) => {
    event.preventDefault();
    const { onSubmit } = this.props;
    onSubmit();
  };

  render() {
    const { action, eventDefinition, streams, onCancel, onChange } = this.props;

    const steps = [
      {
        key: 'event-details',
        title: 'Event Details',
        component: <EventDetailsForm eventDefinition={eventDefinition} onChange={onChange} />,
      },
      {
        key: 'filter-aggregation',
        title: 'Filter & Aggregation',
        component: <FilterAggregationForm eventDefinition={eventDefinition} streams={streams} onChange={onChange} />,
      },
      {
        key: 'fields',
        title: 'Fields',
        component: <FieldsForm eventDefinition={eventDefinition} onChange={onChange} />,
      },
      {
        key: 'notifications',
        title: 'Notifications',
        component: <NotificationsForm eventDefinition={eventDefinition} onChange={onChange} />,
      },
      {
        key: 'summary',
        title: 'Summary',
        component: <EventDefinitionSummary eventDefinition={eventDefinition} />,
      },
    ];

    return (
      <Wizard steps={steps}
              horizontal
              justified
              navigationClassName={styles.steps}
              containerClassName=""
              hidePreviousNextButtons />
    );
  }
}

export default EventDefinitionForm;
