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
import { PluginStore } from 'graylog-web-plugin/plugin';

import type { EventNotification } from 'stores/event-notifications/EventNotificationsStore';

type Props = {
  notification: EventNotification
}

const getNotificationPlugin = (type: string) => {
  if (type === undefined) {
    return {};
  }

  return PluginStore.exports('eventNotificationTypes').find((n) => n.type === type) || { displayName: null };
};

const NotificationConfigTypeCell = ({ notification }: Props) => {
  const plugin = getNotificationPlugin(notification.config.type);

  return (
    <div>{plugin?.displayName || notification.config.type}</div>
  );
};

export default NotificationConfigTypeCell;
