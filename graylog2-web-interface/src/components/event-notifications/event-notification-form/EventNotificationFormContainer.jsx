import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { ConfirmLeaveDialog } from 'components/common';
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
    /** Route needed for ConfirmLeaveDialog to work. This is not needed when embedded in another form. */
    route: PropTypes.object,
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
    route: undefined,
    onSubmit: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      notification: props.notification,
      validation: {
        errors: {},
      },
      isDirty: false,
    };
  }

  handleChange = (key, value) => {
    const { notification } = this.state;
    const nextNotification = lodash.cloneDeep(notification);
    nextNotification[key] = value;
    this.setState({ notification: nextNotification, isDirty: true });
  };

  handleCancel = () => {
    history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
  };

  handleSubmit = () => {
    const { action, embedded, onSubmit } = this.props;
    const { notification } = this.state;

    let promise;
    if (action === 'create') {
      promise = EventNotificationsActions.create(notification);
      promise.then(
        () => {
          this.setState({ isDirty: false }, () => {
            if (!embedded) {
              history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
            }
          });
        },
        (errorResponse) => {
          const { body } = errorResponse.additional;
          if (errorResponse.status === 400 && body && body.failed) {
            this.setState({ validation: body });
          }
        },
      );
    } else {
      promise = EventNotificationsActions.update(notification.id, notification);
      promise.then(
        () => {
          this.setState({ isDirty: false }, () => {
            if (!embedded) {
              history.push(Routes.ALERTS.NOTIFICATIONS.LIST);
            }
          });
        },
        (errorResponse) => {
          const { body } = errorResponse.additional;
          if (errorResponse.status === 400 && body && body.failed) {
            this.setState({ validation: body });
          }
        },
      );
    }

    onSubmit(promise);
  };

  render() {
    const { action, embedded, formId, route } = this.props;
    const { notification, validation, isDirty } = this.state;

    return (
      <React.Fragment>
        {!embedded && isDirty && (
          <ConfirmLeaveDialog route={route}
                              question="Do you really want to abandon this page and lose your changes? This action cannot be undone." />
        )}
        <EventNotificationForm action={action}
                               notification={notification}
                               validation={validation}
                               formId={formId}
                               embedded={embedded}
                               onChange={this.handleChange}
                               onCancel={this.handleCancel}
                               onSubmit={this.handleSubmit} />
      </React.Fragment>
    );
  }
}

export default EventNotificationFormContainer;
