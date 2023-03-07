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
import { useEffect } from 'react';

import { useStore } from 'stores/connect';
import NotificationsFactory from 'logic/notifications/NotificationsFactory';
import type { NotificationType } from 'stores/notifications/NotificationsStore';
import { NotificationsStore, NotificationsActions } from 'stores/notifications/NotificationsStore';

const useNotificationMessage = (notification: NotificationType) => {
  const { messages } = useStore(NotificationsStore);

  useEffect(() => {
    NotificationsActions.getHtmlMessage(notification.type, notification.key, NotificationsFactory.getValuesForNotification(notification));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const key = `${notification.type}-${notification.key}`;

  return messages?.[key];
};

export default useNotificationMessage;
