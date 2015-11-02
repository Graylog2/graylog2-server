import React from 'react';
import { Alert, Button } from 'react-bootstrap';
import moment from 'moment';

const Notification = React.createClass({
  propTypes: {
    notification: React.PropTypes.object.isRequired,
  },
  render() {
    const notification = this.props.notification;
    return (
      <Alert bsStyle="danger" className="notification">
        <Button className="close delete-notification">&times;</Button>

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
