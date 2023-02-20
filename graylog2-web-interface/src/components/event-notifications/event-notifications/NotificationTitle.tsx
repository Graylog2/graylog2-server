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
