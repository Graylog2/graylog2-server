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

import { Spinner } from 'components/common';
import ExpandableCellList from 'components/common/ExpandableCellList';
import EventNotificationLink from 'components/event-notifications/event-notifications/EventNotificationLink';
import useResolvedNotifications from 'components/event-definitions/hooks/useResolvedNotifications';

import type { EventDefinition } from '../event-definitions-types';

type NotificationItem = {
  id: string;
  title: string | undefined;
  isPermitted: boolean;
};

const renderNotification = ({ id, title, isPermitted }: NotificationItem) =>
  isPermitted ? <EventNotificationLink id={id} title={title} /> : <i title="Missing permissions">{id}</i>;

type Props = {
  eventDefinition: EventDefinition;
};

const EventDefinitionNotificationsCell = ({ eventDefinition }: Props) => {
  const { resolved, notPermittedIds, isLoading } = useResolvedNotifications(eventDefinition);

  if (isLoading) {
    return <Spinner text="" />;
  }

  // Notifications the user can't read have no title, so they go last as plain IDs.
  const items: Array<NotificationItem> = [
    ...[...resolved]
      .sort((a, b) => (a.title ?? a.id).localeCompare(b.title ?? b.id))
      .map(({ id, title }) => ({ id, title, isPermitted: true })),
    ...notPermittedIds.map((id) => ({ id, title: undefined, isPermitted: false })),
  ];

  return <ExpandableCellList items={items} renderItem={renderNotification} getItemKey={({ id }) => id} />;
};

export default EventDefinitionNotificationsCell;
