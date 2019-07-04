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
    /** Controls whether the form should be embedded into another one, and submitted/cancel externally */
    embedded: PropTypes.bool,
    /** Controls the ID of the form, so it can be controlled externally */
    formId: PropTypes.string,
    onSubmit: PropTypes.func,
  };

  static defaultProps = {
    action: 'edit',
    notification: {
      title: '',
      description: '',
      config: {},
    },
    embedded: false,
    formId: undefined,
    onSubmit: () => {},
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
    const { action, embedded, onSubmit } = this.props;
    const { notification } = this.state;

    let promise;
    if (action === 'create') {
      promise = EventNotificationsActions.create(notification);
      promise.then(() => {
        if (!embedded) {
          history.push(Routes.NEXT_ALERTS.NOTIFICATIONS.LIST);
        }
      });
    } else {
      promise = EventNotificationsActions.update(notification.id, notification);
      promise.then(() => {
        if (!embedded) {
          history.push(Routes.NEXT_ALERTS.NOTIFICATIONS.LIST);
        }
      });
    }

    onSubmit(promise);
  };

  render() {
    const { action, embedded, formId } = this.props;
    const { notification } = this.state;

    return (
      <EventNotificationForm action={action}
                             notification={notification}
                             formId={formId}
                             embedded={embedded}
                             onChange={this.handleChange}
                             onCancel={this.handleCancel}
                             onSubmit={this.handleSubmit} />
    );
  }
}

export default EventNotificationFormContainer;
