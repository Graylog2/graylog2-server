/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
import { Alert, Row, Col } from 'components/graylog';
import { Icon, Spinner } from 'components/common';
import Notification from 'components/notifications/Notification';

const NotificationsStore = StoreProvider.getStore('Notifications');

const NotificationsList = createReactClass({
  displayName: 'NotificationsList',
  mixins: [Reflux.connect(NotificationsStore)],

  _formatNotificationCount(count) {
    if (count === 0) {
      return 'is no notification';
    }

    if (count === 1) {
      return 'is one notification';
    }

    return `are ${count} notifications`;
  },

  render() {
    if (!this.state.notifications) {
      return <Spinner />;
    }

    const count = this.state.total;

    let title;
    let content;

    if (count === 0) {
      title = 'No notifications';

      content = (
        <Alert bsStyle="success" className="notifications-none">
          <Icon name="check-circle" />{' '}
          &nbsp;No notifications
        </Alert>
      );
    } else {
      title = `There ${this._formatNotificationCount(count)}`;

      content = this.state.notifications.map((notification) => {
        return <Notification key={`${notification.type}-${notification.timestamp}`} notification={notification} />;
      });
    }

    return (
      <Row className="content">
        <Col md={12}>
          <h2>{title}</h2>
          <p className="description">
            Notifications are triggered by Graylog and indicate a situation you should act upon. Many notification
            types will also provide a link to the Graylog documentation if you need more information or assistance.
          </p>

          {content}
        </Col>
      </Row>
    );
  },
});

export default NotificationsList;
