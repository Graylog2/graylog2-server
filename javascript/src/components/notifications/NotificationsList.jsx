import React from 'react';
import Reflux from 'reflux';
import { Alert, Row, Col } from 'react-bootstrap';

import NotificationsStore from 'stores/notifications/NotificationsStore';

import { Spinner } from 'components/common';
import Notification from 'components/notifications/Notification';

const NotificationsList = React.createClass({
  mixins: [Reflux.connect(NotificationsStore)],
  _formatNotificationCount(count) {
    if (count === 0) {
      return 'is no notification';
    }
    if (count === 1) {
      return 'is one notification';
    }

    return 'are ' + count + ' notifications';

  },
  render() {
    if (!this.state.notifications) {
      return <Spinner />;
    }

    const count = this.state.total;

    if (count === 0) {
      return (
        <Alert bsStyle="success" className="notifications-none">
          <i className="fa fa-check-circle"/>{' '}
          &nbsp;No notifications
        </Alert>
      );
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2>There {this._formatNotificationCount(count)}</h2>
          <p className="description">
            Notifications are triggered by Graylog and indicate a situation you should act upon. Many notification
            types will also provide a link to the Graylog documentation if you need more information or assistance.
          </p>

          {this.state.notifications.map((notification) => <Notification key={notification.id} notification={notification} />)}
        </Col>
      </Row>
    );
  },
});

export default NotificationsList;
