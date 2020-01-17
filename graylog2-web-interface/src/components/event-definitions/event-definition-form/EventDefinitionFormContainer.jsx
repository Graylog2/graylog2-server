import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { withRouter } from 'react-router';

import history from 'util/History';
import Routes from 'routing/Routes';
import connect from 'stores/connect';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import CombinedProvider from 'injection/CombinedProvider';

import { ConfirmLeaveDialog, Spinner } from 'components/common';
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
    route: PropTypes.object.isRequired,
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
      isDirty: false,
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
      return { eventDefinition: nextEventDefinition, isDirty: true };
    });
  };

  handleCancel = () => {
    history.push(Routes.ALERTS.DEFINITIONS.LIST);
  };

  _validate = (eventDefinition) => {
    const validation = {
      isValid: false,
    };

    const { query_parameters: queryParameters } = eventDefinition.config;
    if (queryParameters.length > 0 && queryParameters.some(p => p.embryonic)) {
      const undeclaredParameters = queryParameters.filter(p => p.embryonic)
        .map(p => p.name)
        .join(', ');
      validation.results = {
        errors: {
          query_parameters: [`Undeclared parameters: ${undeclaredParameters}.`],
        },
      };
      return validation;
    }

    return { isValid: true };
  };

  handleSubmitSuccessResponse = () => {
    this.setState({ isDirty: false }, () => history.push(Routes.ALERTS.DEFINITIONS.LIST));
  };

  handleSubmitFailureResponse = (errorResponse) => {
    const { body } = errorResponse.additional;
    if (errorResponse.status === 400) {
      if (body && body.failed) {
        this.setState({ validation: body });
        return;
      }
      if (body.type && body.type === 'ApiError') {
        if (body.message.includes('org.graylog.events.conditions.Expression')
        || body.message.includes('org.graylog.events.conditions.Expr')
        || body.message.includes('org.graylog.events.processor.aggregation.AggregationSeries')) {
          this.setState({
            validation: {
              errors: { conditions: ['Aggregation condition is not valid'] },
            },
          });
        }
      }
    }
  };

  handleSubmit = () => {
    const { action } = this.props;
    const { eventDefinition } = this.state;
    const validation = this._validate(eventDefinition);
    if (!validation.isValid) {
      this.setState({ validation: validation.results });
      return;
    }

    if (action === 'create') {
      EventDefinitionsActions.create(eventDefinition)
        .then(this.handleSubmitSuccessResponse, this.handleSubmitFailureResponse);
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(this.handleSubmitSuccessResponse, this.handleSubmitFailureResponse);
    }
  };

  render() {
    const { action, entityTypes, notifications, currentUser, route } = this.props;
    const { isDirty, eventDefinition, eventsClusterConfig, validation } = this.state;
    const isLoading = !entityTypes || !notifications.all || !eventsClusterConfig;

    if (isLoading) {
      return <Spinner text="Loading Event information..." />;
    }

    const defaults = { default_backlog_size: eventsClusterConfig.events_notification_default_backlog };

    return (
      <React.Fragment>
        {isDirty && (
          <ConfirmLeaveDialog route={route}
                              question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
        )}
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
      </React.Fragment>
    );
  }
}

export default connect(withRouter(EventDefinitionFormContainer), {
  entityTypes: AvailableEventDefinitionTypesStore,
  notifications: EventNotificationsStore,
  currentUser: CurrentUserStore,
},
({ currentUser, ...otherProps }) => ({
  ...otherProps,
  currentUser: currentUser.currentUser,
}));
