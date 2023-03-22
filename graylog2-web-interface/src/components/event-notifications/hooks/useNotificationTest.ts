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
import { useState } from 'react';

import type { EventNotification, TestResults } from 'stores/event-notifications/EventNotificationsStore';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';

type UseNotificationTestType = {
  isLoadingTest: boolean,
  testResults: TestResults
  getNotificationTest: (notification: EventNotification) => void,
}

const useNotificationTest = (): UseNotificationTestType => {
  const [testResults, setTestResults] = useState(undefined);

  const getNotificationTest = (notification: EventNotification) => {
    setTestResults({ [notification.id]: { isLoading: true, id: notification.id } });
    let result = { isLoading: false, id: null, error: null, message: null };

    EventNotificationsActions.testPersisted(notification)
      .then(
        (response) => {
          result = {
            ...result,
            id: notification.id,
            error: false,
            message: 'Notification was executed successfully.',
          };

          setTestResults({ [notification.id]: result });

          return response;
        },
        (errorResponse) => {
          result = { isLoading: false, id: notification.id, error: true, message: null };

          if (errorResponse.status !== 400 || !errorResponse.additional.body || !errorResponse.additional.body.failed) {
            result.message = errorResponse.responseMessage || 'Unknown errorResponse, please check your Graylog server logs.';
          }

          setTestResults({ [notification.id]: result });

          return errorResponse;
        },
      );
  };

  return {
    isLoadingTest: testResults?.isLoading,
    testResults,
    getNotificationTest,
  };
};

export default useNotificationTest;
