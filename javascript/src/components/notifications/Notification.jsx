import React from 'react';
import { Alert, Button } from 'react-bootstrap';
import moment from 'moment';

import NotificationsActions from 'actions/notifications/NotificationsActions';

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
    return (
      <Alert bsStyle="danger" className="notification">
        <Button className="close delete-notification" onClick={this._onClose}>&times;</Button>

        <h3 className="notification-head">
          <i className="fa fa-bolt"/>{' '}
          {notification.type}{' '}

          <span className="notification-timestamp">
            (triggered {moment(notification.timestamp).fromNow()})
          </span>
        </h3>
      </Alert>
    );
  },
});

export default Notification;
