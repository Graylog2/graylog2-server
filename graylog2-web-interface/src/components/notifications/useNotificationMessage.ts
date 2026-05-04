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
import type { LegacyNotificationType, NotificationType } from 'components/notifications/types';
import { NOTIFICATIONS_QUERY_KEY } from 'components/notifications/constants';

type Type = Parameters<typeof SystemNotificationMessage.renderHtmlWithKey>[0];

// Accepts either the new SystemNotificationDto (id-bearing) or the legacy
// listNotifications() row (no id). Phase 5 deletes the legacy callers; until
// then we keep this hook compatible with both shapes.
type AnyNotification = NotificationType | LegacyNotificationType;

// TODO(1.14 follow-up): swap fetcher to GET /system/notifications/{id}/message/html
// once @graylog/server-api types regenerate from the deployed backend (Patrick's
// PR graylog2-server#25873). Until then, we keep the legacy renderHtml/WithKey
// calls so this hook keeps working against the current SDK shape.
const fetchNotificationMessage = (notification: AnyNotification) => {
  const values = NotificationsFactory.getValuesForNotification(notification);
  const type = notification.type.toLocaleUpperCase() as Type;

  return notification.key
    ? SystemNotificationMessage.renderHtmlWithKey(type, notification.key, values)
    : SystemNotificationMessage.renderHtml(type, values);
};

const useNotificationMessage = (notification: AnyNotification) => {
  /*
   * Cache key MUST disambiguate notifications of the same type — see Spec OQ-FE-1.
   * The previous key ('message', notification.type) collided across notifications
   * of the same type but different ids/keys. The new SystemNotificationDto carries
   * `id`; the legacy listNotifications() shape only has `key`. We include both
   * (and `type`) so the entry stays unique on either shape, satisfying
   * @tanstack/query/exhaustive-deps without spreading the full `notification`
   * reference (which would defeat the cache by rerendering as a new object).
   */
  const id = 'id' in notification ? notification.id : undefined;
  const { data } = useQuery({
    // eslint-disable-next-line @tanstack/query/exhaustive-deps
    queryKey: [
      ...NOTIFICATIONS_QUERY_KEY,
      'message',
      id,
      notification.key,
      notification.type,
    ],
    queryFn: () => fetchNotificationMessage(notification),
  });

  return data;
};

export default useNotificationMessage;
