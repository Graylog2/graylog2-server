import React from 'react';
import { Alert, Button } from 'react-bootstrap';
import { Timestamp } from 'components/common';

import NotificationsFactory from 'logic/notifications/NotificationsFactory';

import ActionsProvider from 'injection/ActionsProvider';
const NotificationsActions = ActionsProvider.getActions('Notifications');

const Notification = React.createClass({
  propTypes: {
    notification: React.PropTypes.object.isRequired,
  },
  _onClose() {
    if (window.confirm('Really delete this notification?')) {
      NotificationsActions.delete(this.props.notification.type);
    }
  },
  render() {
    const notification = this.props.notification;
    const notificationView = NotificationsFactory.getForNotification(notification);
    return (
      <Alert bsStyle="danger" className="notification">
        <Button className="close delete-notification" onClick={this._onClose}>&times;</Button>

        <h3 className="notification-head">
          <i className="fa fa-bolt" />{' '}
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
  },
});

export default Notification;
