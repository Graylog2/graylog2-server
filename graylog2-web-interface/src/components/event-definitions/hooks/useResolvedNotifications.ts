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
import useNotificationsByIds from 'components/event-notifications/hooks/useNotificationsByIds';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

export type ResolvedNotification = {
  id: string;
  title: string | undefined;
  found: boolean;
};

const useResolvedNotifications = (eventDefinition: EventDefinition): ResolvedNotification[] => {
  const ids = eventDefinition.notifications.map(({ notification_id }) => notification_id);
  const { data: notifications } = useNotificationsByIds(ids);
  const byId = Object.fromEntries((notifications ?? []).map((n) => [n.id, n]));

  return ids.map((id) => {
    const found = byId[id];

    return {
      id,
      title: found?.title,
      found: Boolean(found),
    };
  });
};

export default useResolvedNotifications;
