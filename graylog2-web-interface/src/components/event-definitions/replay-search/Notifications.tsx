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
import { useEffect, useMemo } from 'react';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { useStore } from 'stores/connect';
import { EventNotificationsStore, EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import NoAttributeProvided from 'components/event-definitions/replay-search/NoAttributeProvided';
import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';

import useAlertAndEventDefinitionData from './hooks/useAlertAndEventDefinitionData';

const Notifications = () => {
  const { alertId, definitionId } = useReplaySearchContext();
  const { eventDefinition } = useAlertAndEventDefinitionData(alertId, definitionId);

  useEffect(() => {
    EventNotificationsActions.listAll();
  }, []);

  const allNotifications = useStore(EventNotificationsStore, ({ all }) => Object.fromEntries(
    (all ?? []).map((notification) => [notification.id, notification]),
  ));

  const notificationList = useMemo(() => eventDefinition.notifications
    .flatMap(({ notification_id }) => (allNotifications[notification_id] ? [allNotifications[notification_id]] : [])),
  [allNotifications, eventDefinition.notifications]);

  return notificationList.length ? (
    <>
      {notificationList.map(({ id, title }, index) => {
        const prefix = index > 0 ? ', ' : '';

        return (
          <span key={id}>
            {prefix}
            <Link target="_blank" to={Routes.ALERTS.NOTIFICATIONS.show(id)}>{title}</Link>
          </span>
        );
      })}
    </>
  ) : <NoAttributeProvided name="Notifications" />;
};

export default Notifications;
