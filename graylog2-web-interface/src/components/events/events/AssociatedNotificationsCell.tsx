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
import styled from 'styled-components';

import usePermissions from 'hooks/usePermissions';
import Spinner from 'components/common/Spinner';
import ExpandableListCell from 'components/common/ExpandableListCell';
import EventNotificationLink from 'components/event-notifications/event-notifications/EventNotificationLink';
import useNotificationsByIds from 'components/event-notifications/hooks/useNotificationsByIds';
import useEventDefinitionBatched from 'components/event-definitions/hooks/useEventDefinitionBatched';

// Link-styled buttons in other list cells wrap long labels on their own; plain links need the same rules to match.
const NotificationEntry = styled.span`
  white-space: normal;
  word-break: break-word;
`;

// Shows the event definition's current notifications, not the ones that fired for this specific event.
const AssociatedNotificationsCell = ({ eventDefinitionId }: { eventDefinitionId: string }) => {
  const { isPermitted } = usePermissions();
  const canReadEventDefinition = isPermitted(`eventdefinitions:read:${eventDefinitionId}`);
  const { data: eventDefinition, isLoading: isLoadingEventDefinition } = useEventDefinitionBatched(
    canReadEventDefinition ? eventDefinitionId : undefined,
  );
  const notificationIds = (eventDefinition?.notifications ?? []).map(({ notification_id }) => notification_id);
  const { data: notifications = [], notPermittedIds, isLoading } = useNotificationsByIds(notificationIds);

  if (!canReadEventDefinition || isLoadingEventDefinition) return null;
  if (isLoading) return <Spinner />;

  const items = [
    ...[...notifications]
      .sort((a, b) => (a.title ?? a.id).localeCompare(b.title ?? b.id))
      .map(({ id, title }) => (
        <NotificationEntry key={id}>
          <EventNotificationLink id={id} title={title} />
        </NotificationEntry>
      )),
    // Notifications the user can't read have no title, so they go last as plain IDs.
    ...notPermittedIds.map((id) => (
      <NotificationEntry key={id}>
        <i>{id}</i>
      </NotificationEntry>
    )),
  ];

  return <ExpandableListCell items={items} />;
};

export default AssociatedNotificationsCell;
