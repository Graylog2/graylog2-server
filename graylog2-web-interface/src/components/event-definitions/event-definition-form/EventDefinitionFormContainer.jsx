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
const { ConfigurationActions } = CombinedProvider.get('Configuration');
const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

class EventDefinitionFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object,
    currentUser: PropTypes.object.isRequired,
    entityTypes: PropTypes.object,
    notifications: PropTypes.object.isRequired,
    onEventDefinitionChange: PropTypes.func,
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
      notification_settings: {
        grace_period_ms: 0,
        // Defaults to system setting for notification backlog size
        backlog_size: null,
      },
      notifications: [],
      alert: false,
    },
    entityTypes: undefined,
    onEventDefinitionChange: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinition: props.eventDefinition,
      validation: {
        errors: {},
      },
      eventsClusterConfig: undefined,
    };
  }

  componentDidMount() {
    this.fetchClusterConfig();
    this.fetchNotifications();
  }

  fetchNotifications = () => {
    EventNotificationsActions.listAll();
  };

  fetchClusterConfig = () => {
    ConfigurationActions.listEventsClusterConfig().then(config => this.setState({ eventsClusterConfig: config }));
  };

  handleChange = (key, value) => {
    this.setState((state) => {
      const nextEventDefinition = lodash.cloneDeep(state.eventDefinition);
      nextEventDefinition[key] = value;
      const { onEventDefinitionChange } = this.props;
      onEventDefinitionChange(nextEventDefinition);
      return { eventDefinition: nextEventDefinition };
    });
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
        .then(
          () => history.push(Routes.ALERTS.DEFINITIONS.LIST),
          (errorResponse) => {
            const { body } = errorResponse.additional;
            if (errorResponse.status === 400 && body && body.failed) {
              this.setState({ validation: body });
            }
          },
        );
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(
          () => history.push(Routes.ALERTS.DEFINITIONS.LIST),
          (errorResponse) => {
            const { body } = errorResponse.additional;
            if (errorResponse.status === 400 && body && body.failed) {
              this.setState({ validation: body });
            }
          },
        );
    }
  };

  render() {
    const { action, entityTypes, notifications, currentUser } = this.props;
    const { eventDefinition, eventsClusterConfig, validation } = this.state;
    const isLoading = !entityTypes || !notifications.all || !eventsClusterConfig;

    if (isLoading) {
      return <Spinner text="Loading Event information..." />;
    }

    const defaults = { default_backlog_size: eventsClusterConfig.events_notification_default_backlog };

    return (
      <EventDefinitionForm action={action}
                           eventDefinition={eventDefinition}
                           currentUser={currentUser}
                           validation={validation}
                           entityTypes={entityTypes}
                           notifications={notifications.all}
                           defaults={defaults}
                           onChange={this.handleChange}
                           onCancel={this.handleCancel}
                           onSubmit={this.handleSubmit} />
    );
  }
}

export default connect(EventDefinitionFormContainer, {
  entityTypes: AvailableEventDefinitionTypesStore,
  notifications: EventNotificationsStore,
  currentUser: CurrentUserStore,
},
({ currentUser, ...otherProps }) => ({
  ...otherProps,
  currentUser: currentUser.currentUser,
}));
