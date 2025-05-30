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
import { useQuery } from '@tanstack/react-query';

import { SystemNotificationMessage } from '@graylog/server-api';

import NotificationsFactory from 'components/notifications/NotificationsFactory';
import type { NotificationType } from 'components/notifications/types';
import { NOTIFICATIONS_QUERY_KEY } from 'components/notifications/constants';

const useNotificationMessage = (notification: NotificationType) => {
  const { data } = useQuery({
    queryKey: [...NOTIFICATIONS_QUERY_KEY, 'message', notification.type],
    queryFn: () =>
      SystemNotificationMessage.renderHtmlWithKey(
        // @ts-expect-error Should be fixed
        notification.type.toLocaleUpperCase(),
        notification.key,
        NotificationsFactory.getValuesForNotification(notification),
      ),
  });

  return data;
};

export default useNotificationMessage;
