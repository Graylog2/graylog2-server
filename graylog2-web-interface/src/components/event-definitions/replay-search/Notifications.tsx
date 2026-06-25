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
import * as React from 'react';
import { useMemo } from 'react';

import { useEventNotifications } from 'components/event-notifications/hooks/useEventNotifications';
import NoAttributeProvided from 'components/event-definitions/replay-search/NoAttributeProvided';
import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';
import EventNotificationLink from 'components/event-notifications/event-notifications/EventNotificationLink';

import useAlertAndEventDefinitionData from './hooks/useAlertAndEventDefinitionData';

const Notifications = () => {
  const { alertId, definitionId } = useReplaySearchContext();
  const { eventDefinition } = useAlertAndEventDefinitionData(alertId, definitionId);
  const { data: notificationsData } = useEventNotifications();

  const allNotifications = useMemo(
    () =>
      Object.fromEntries(
        (notificationsData?.notifications ?? []).map((notification) => [notification.id, notification]),
      ),
    [notificationsData],
  );

  const notificationList = useMemo(
    () =>
      eventDefinition.notifications.flatMap(({ notification_id }) =>
        allNotifications[notification_id] ? [allNotifications[notification_id]] : [],
      ),
    [allNotifications, eventDefinition.notifications],
  );

  return notificationList.length ? (
    <>
      {notificationList.map(({ id, title }, index) => {
        const prefix = index > 0 ? ', ' : '';

        return (
          <span key={id}>
            {prefix}
            <EventNotificationLink id={id} title={title} />
          </span>
        );
      })}
    </>
  ) : (
    <NoAttributeProvided />
  );
};

export default Notifications;
