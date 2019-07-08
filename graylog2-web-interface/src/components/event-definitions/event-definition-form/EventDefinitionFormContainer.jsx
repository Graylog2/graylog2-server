import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import history from 'util/History';
import Routes from 'routing/Routes';
import connect from 'stores/connect';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import CombinedProvider from 'injection/CombinedProvider';

import { Spinner } from 'components/common';
import EventDefinitionForm from './EventDefinitionForm';

// Import built-in plugins
import {} from 'components/event-definitions/event-definition-types';
import {} from 'components/event-notifications/event-notification-types';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { AvailableEventDefinitionTypesStore } = CombinedProvider.get('AvailableEventDefinitionTypes');
const { EventNotificationsStore, EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class EventDefinitionFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object,
    entityTypes: PropTypes.object,
    notifications: PropTypes.object.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    eventDefinition: {
      title: '',
      description: '',
      priority: EventDefinitionPriorityEnum.NORMAL,
      config: {},
      field_spec: {},
      key_spec: [],
      action_settings: { grace_period_ms: 0 },
      actions: [],
      alert: false,
    },
    entityTypes: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinition: props.eventDefinition,
    };
  }

  componentDidMount() {
    this.fetchNotifications();
  }

  fetchNotifications = () => {
    EventNotificationsActions.listAll();
  };

  handleChange = (key, value) => {
    const { eventDefinition } = this.state;
    const nextEventDefinition = lodash.cloneDeep(eventDefinition);
    nextEventDefinition[key] = value;
    this.setState({ eventDefinition: nextEventDefinition });
  };

  handleCancel = () => {
    if (window.confirm('Do you really want to abandon this page and lose your changes? This action cannot be undone.')) {
      history.goBack();
    }
  };

  handleSubmit = () => {
    const { action } = this.props;
    const { eventDefinition } = this.state;

    if (action === 'create') {
      EventDefinitionsActions.create(eventDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    }
  };

  render() {
    const { action, entityTypes, notifications } = this.props;
    const { eventDefinition } = this.state;
    const isLoading = !entityTypes || !notifications.all;

    if (isLoading) {
      return <Spinner text="Loading Event information..." />;
    }

    return (
      <EventDefinitionForm action={action}
                           eventDefinition={eventDefinition}
                           entityTypes={entityTypes}
                           notifications={notifications.all}
                           onChange={this.handleChange}
                           onCancel={this.handleCancel}
                           onSubmit={this.handleSubmit} />
    );
  }
}

export default connect(EventDefinitionFormContainer, {
  entityTypes: AvailableEventDefinitionTypesStore,
  notifications: EventNotificationsStore,
});
