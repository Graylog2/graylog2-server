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

import type { EventNotification, TestResults } from 'stores/event-notifications/EventNotificationsStore';
import { Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

type Props = {
  notification: EventNotification
  testResults: TestResults
}

const NotificationTitle = ({ notification, testResults }: Props) => {
  const result = testResults?.[notification.id];

  return (
    <>
      <Link to={Routes.ALERTS.NOTIFICATIONS.show(notification.id)}>{notification.title}</Link>
      {result?.id === notification.id ? (
        <div>
          {result.isLoading ? (
            <Spinner text="Testing Notification..." />
          ) : (
            <p className={result.error ? 'text-danger' : 'text-success'}>
              <b>{result.error ? 'Error' : 'Success'}:</b> {result.message}
            </p>
          )}
        </div>
      ) : null}
    </>
  );
};

export default NotificationTitle;
