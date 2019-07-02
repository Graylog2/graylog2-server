import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import history from 'util/History';
import Routes from 'routing/Routes';

import CombinedProvider from 'injection/CombinedProvider';

import EventNotificationForm from './EventNotificationForm';

// Import built-in Event Notification Types
import {} from '../event-notification-types';

const { EventNotificationsActions } = CombinedProvider.get('EventNotifications');

class EventNotificationFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    notification: PropTypes.object,
  };

  static defaultProps = {
    action: 'edit',
    notification: {
      title: '',
      description: '',
      config: {},
    },
  };

  constructor(props) {
    super(props);

    this.state = {
      notification: props.notification,
    };
  }

  handleChange = (key, value) => {
    const { notification } = this.state;
    const nextNotification = lodash.cloneDeep(notification);
    nextNotification[key] = value;
    this.setState({ notification: nextNotification });
  };

  handleCancel = () => {
    if (window.confirm('Do you really want to abandon this page and lose your changes? This action cannot be undone.')) {
      history.goBack();
    }
  };

  handleSubmit = () => {
    const { action } = this.props;
    const { notification } = this.state;

    if (action === 'create') {
      EventNotificationsActions.create(notification)
        .then(() => history.push(Routes.NEXT_ALERTS.NOTIFICATIONS.LIST));
    } else {
      EventNotificationsActions.update(notification.id, notification)
        .then(() => history.push(Routes.NEXT_ALERTS.NOTIFICATIONS.LIST));
    }
  };

  render() {
    const { action } = this.props;
    const { notification } = this.state;

    return (
      <EventNotificationForm action={action}
                             notification={notification}
                             onChange={this.handleChange}
                             onCancel={this.handleCancel}
                             onSubmit={this.handleSubmit} />
    );
  }
}

export default EventNotificationFormContainer;
