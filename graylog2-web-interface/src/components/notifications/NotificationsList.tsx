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

import { Alert, Row, Col } from 'components/bootstrap';
import { Spinner } from 'components/common';
import Notification from 'components/notifications/Notification';
import type { NotificationType } from 'components/notifications/types';

import useNotifications from './useNotifications';

const _formatNotificationCount = (count: number) => {
  if (count === 1) {
    return 'is one notification';
  }

  return `are ${count} notifications`;
};

const Title = ({ count }: { count: number }) =>
  count === 0 ? 'No notifications' : `There ${_formatNotificationCount(count)}`;

const Notifications = ({ count, notifications }: { count: number; notifications: Array<NotificationType> }) =>
  count === 0 ? (
    <Alert bsStyle="success" className="notifications-none">
      No notifications
    </Alert>
  ) : (
    notifications?.map((notification) => (
      <Notification
        key={`${notification.type}-${notification?.key}-${notification.timestamp}`}
        notification={notification}
      />
    ))
  );

const NotificationsList = () => {
  const { data, isLoading } = useNotifications();

  if (isLoading) {
    return <Spinner />;
  }

  const { total, notifications } = data;

  return (
    <Row className="content">
      <Col md={12}>
        <h2>
          <Title count={total} />
        </h2>
        <p className="description">
          Notifications indicate a situation you should act upon. Many notification types will also provide a link to
          the documentation if you need more information or assistance.
        </p>

        <Notifications count={total} notifications={notifications} />
      </Col>
    </Row>
  );
};

export default NotificationsList;
