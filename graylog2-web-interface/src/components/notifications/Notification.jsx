import PropTypes from 'prop-types';
import React from 'react';

import { Alert, Button } from 'components/graylog';
import { Timestamp, Icon } from 'components/common';

import NotificationsFactory from 'logic/notifications/NotificationsFactory';

import ActionsProvider from 'injection/ActionsProvider';

const NotificationsActions = ActionsProvider.getActions('Notifications');

class Notification extends React.Component {
  static propTypes = {
    notification: PropTypes.object.isRequired,
  };

  _onClose = () => {
    if (window.confirm('Really delete this notification?')) {
      NotificationsActions.delete(this.props.notification.type);
    }
  };

  render() {
    const { notification } = this.props;
    const notificationView = NotificationsFactory.getForNotification(notification);
    return (
      <Alert bsStyle="danger" className="notification">
        <Button className="close delete-notification" onClick={this._onClose}>&times;</Button>

        <h3 className="notification-head">
          <Icon name="bolt" />{' '}
          {notificationView.title}{' '}

          <span className="notification-timestamp">
            (triggered <Timestamp dateTime={notification.timestamp} relative />)
          </span>
        </h3>
        <div className="notification-description">
          {notificationView.description}
        </div>
      </Alert>
    );
  }
}

export default Notification;
